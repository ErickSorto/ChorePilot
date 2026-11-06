import copy
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
import release
import release_generation as generation


class GenerationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "app").mkdir()
        (self.root / "release").mkdir()
        self.config = {
            "app_name": "Test App", "source_locale": "en-US",
            "locales": {"en-US": {"android": "en-US", "ios": "en-US"},
                        "fr-FR": {"android": "fr-FR", "ios": "fr-FR"}},
            "notes_generation": {"source_paths": ["app"]},
        }
        (self.root / "release/config.json").write_text(json.dumps(self.config))
        (self.root / "app/example.txt").write_text("before")
        self.git("init", "-q")
        self.commit("Initial app")
        self.base = self.git("rev-parse", "HEAD").strip()
        (self.root / "app/example.txt").write_text("rounded button feedback")
        self.commit("Fix feedback outside rounded buttons")
        self.head = self.git("rev-parse", "HEAD").strip()
        self.manager = release.ReleaseManager(self.root)
        self.path = self.manager.init("1.2.3", 53, 8)
        self.answer = {"notes": {"en-US": "Improved rounded button feedback.",
                                 "fr-FR": "Amélioration de l’effet des boutons arrondis à l’appui."},
                       "warnings": []}

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args], text=True)

    def commit(self, subject):
        self.git("add", ".")
        self.git("-c", "user.name=Release Test", "-c", "user.email=test@example.invalid",
                 "commit", "-qm", subject)

    def run_generation(self, answer=None, runner=None, **options):
        result = copy.deepcopy(self.answer if answer is None else answer)
        return generation.generate(self.manager, "1.2.3", self.base, "HEAD",
                                   runner=runner or (lambda *a, **kw: result), **options)

    def test_generates_all_locales_into_existing_manifest_and_preview_without_review(self):
        original = json.loads(self.path.read_text())
        original["store"]["checks"] = {"privacy_current": True}
        self.path.write_text(json.dumps(original))
        preview = self.run_generation()
        manifest = self.manager.validate("1.2.3")
        self.assertEqual(manifest["notes"], self.answer["notes"])
        self.assertEqual(manifest["store"], original["store"])
        self.assertEqual(manifest["reviews"], {})
        self.assertNotIn("store_review", manifest)
        self.assertEqual(manifest["generation"]["head_commit"], self.head)
        self.assertIn("Store inputs reviewed: no", preview.read_text())
        with self.assertRaisesRegex(ValueError, "Review missing"):
            self.manager.export("1.2.3", reviewed=True)

    def test_same_notes_export_to_both_stores_and_testflight_after_review(self):
        self.run_generation()
        for locale in self.manager.locales:
            self.manager.review("1.2.3", locale)
        out = self.manager.export("1.2.3", reviewed=True)
        self.assertEqual((out / "metadata/android/fr-FR/changelogs/53.txt").read_text(), self.answer["notes"]["fr-FR"])
        self.assertEqual((out / "metadata/ios/fr-FR/release_notes.txt").read_text(), self.answer["notes"]["fr-FR"])
        self.assertEqual(json.loads((out / "testflight.json").read_text())["localized_build_info"]["fr-FR"]["whats_new"], self.answer["notes"]["fr-FR"])

    def test_existing_notes_require_explicit_replacement_and_clear_reviews(self):
        self.run_generation()
        self.manager.review_all("1.2.3")
        value = json.loads(self.path.read_text()); value["store_review"] = {"stale": True}
        self.path.write_text(json.dumps(value))
        before = self.path.read_bytes()
        with self.assertRaisesRegex(ValueError, "replace-drafts"):
            self.run_generation()
        self.assertEqual(before, self.path.read_bytes())
        self.run_generation(replace=True)
        self.assertEqual(self.manager.load("1.2.3")["reviews"], {})
        self.assertNotIn("store_review", self.manager.load("1.2.3"))

    def test_rejects_bad_schema_missing_extra_locale_lengths_and_placeholders_atomically(self):
        before = self.path.read_bytes()
        cases = [[], {}, {**self.answer, "reviews": {}},
                 {"notes": {"en-US": "Hello"}, "warnings": []},
                 {"notes": {**self.answer["notes"], "de-DE": "Hallo"}, "warnings": []},
                 {"notes": {**self.answer["notes"], "fr-FR": "x" * 501}, "warnings": []},
                 {"notes": {**self.answer["notes"], "en-US": "TODO"}, "warnings": []},
                 {"notes": {**self.answer["notes"], "en-US": 123}, "warnings": []}]
        for answer in cases:
            with self.subTest(answer=answer), self.assertRaises(ValueError):
                self.run_generation(answer=answer)
            self.assertEqual(before, self.path.read_bytes())

    def test_uncertain_facts_save_local_warnings_without_changing_manifest(self):
        before = self.path.read_bytes()
        with self.assertRaisesRegex(ValueError, "generation-warnings.json"):
            self.run_generation(answer={**self.answer, "warnings": ["Bulk commit needs reviewed user-visible facts."]})
        self.assertEqual(before, self.path.read_bytes())
        self.assertTrue((self.root / "build/release/1.2.3/generation-warnings.json").is_file())

    def test_dirty_and_untracked_source_are_rejected_before_model_call(self):
        with patch.object(generation, "run_assistant") as model:
            (self.root / "app/untracked.txt").write_text("unknown scope")
            with self.assertRaisesRegex(ValueError, "Commit or isolate"):
                self.run_generation(runner=model)
            model.assert_not_called()
        (self.root / "app/untracked.txt").unlink()
        (self.root / "app/example.txt").write_text("not committed")
        with self.assertRaisesRegex(ValueError, "Commit or isolate"):
            self.run_generation()

    def test_source_drift_blocks_review_and_export_but_can_regenerate(self):
        self.run_generation()
        (self.root / "app/example.txt").write_text("new code")
        self.commit("Improve calendar spacing")
        for action in (lambda: self.manager.review("1.2.3", "en-US"),
                       lambda: self.manager.export("1.2.3")):
            with self.assertRaisesRegex(ValueError, "source changed"):
                action()
        self.run_generation(replace=True)
        self.manager.validate("1.2.3")

    def test_notes_only_commit_does_not_stale_source_binding(self):
        self.run_generation()
        self.commit("Add draft release notes")
        self.manager.validate("1.2.3")

    def test_changed_configuration_requires_regeneration(self):
        self.run_generation()
        self.manager.config["app_name"] = "Renamed App"
        with self.assertRaisesRegex(ValueError, "configuration changed"):
            self.manager.validate("1.2.3")

    def test_explicit_baseline_and_checked_out_head_required(self):
        for base, head in [("--help", "HEAD"), ("HEAD", "HEAD"), (self.head, self.base)]:
            with self.subTest(base=base, head=head), self.assertRaises(ValueError):
                generation.evidence(self.manager, base, head)
        self.commit("Release inputs only")
        with self.assertRaisesRegex(ValueError, "No app-source changes"):
            generation.evidence(self.manager, self.head, "HEAD")

    def test_generation_does_not_overwrite_concurrent_manifest_edits(self):
        def run(*args, **kwargs):
            value = json.loads(self.path.read_text()); value["ios_build_number"] = 9
            self.path.write_text(json.dumps(value)); return self.answer
        with self.assertRaisesRegex(ValueError, "inputs changed"):
            self.run_generation(runner=run)
        self.assertEqual(json.loads(self.path.read_text())["ios_build_number"], 9)
        self.assertEqual(json.loads(self.path.read_text())["notes"]["en-US"], "")

    def test_facts_are_explicit_and_prompt_contains_no_source_contents(self):
        facts = self.root / "facts.txt"; facts.write_text("Button feedback follows rounded corners.")
        data = generation.evidence(self.manager, self.base, "HEAD", facts)
        prompt = generation.generation_prompt(self.manager, data)
        self.assertIn("Button feedback follows rounded corners.", prompt)
        self.assertIn("untrusted DATA", prompt)
        self.assertNotIn("test@example.invalid", prompt)
        self.assertNotIn('"rounded button feedback"', prompt)

    def test_source_path_escape_and_git_pathspecs_are_rejected(self):
        for paths in (["../private"], [":(top)secret"], ["release"], ["*.txt"], [], "app"):
            self.manager.config["notes_generation"] = {"source_paths": paths}
            with self.subTest(paths=paths), self.assertRaises(ValueError):
                generation.scope(self.manager)


class AssistantAdapterTests(unittest.TestCase):
    @patch.object(generation, "find_assistant", return_value="/existing-assistant")
    def test_read_only_chatgpt_structured_invocation_has_no_api_key_fallback(self, binary):
        calls = []
        def run(command, **kwargs):
            calls.append((command, kwargs))
            if command[1:3] == ["login", "status"]:
                return subprocess.CompletedProcess(command, 0, "", "Logged in using ChatGPT")
            output = Path(command[command.index("--output-last-message") + 1])
            output.write_text('{"notes":{},"warnings":[]}')
            return subprocess.CompletedProcess(command, 0, "", "")
        with patch.object(generation.subprocess, "run", side_effect=run), patch.dict(os.environ, {"OPENAI_API_KEY": "not-to-use"}):
            self.assertEqual(generation.run_assistant("facts", {}, timeout=20), {"notes": {}, "warnings": []})
        command, kwargs = calls[-1]
        for flag in ("--ignore-user-config", "--ephemeral", "--output-schema", "read-only", 'forced_login_method="chatgpt"'):
            self.assertIn(flag, command)
        self.assertNotIn("OPENAI_API_KEY", kwargs["env"])
        self.assertEqual(kwargs["input"], "facts")
        self.assertNotIn("shell", kwargs)

    @patch.object(generation, "find_assistant", return_value="/existing-assistant")
    def test_api_key_login_is_rejected_without_generation(self, binary):
        with patch.object(generation.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "Logged in using an API key", "")) as run:
            with self.assertRaisesRegex(ValueError, "ChatGPT CLI login"):
                generation.run_assistant("facts", {})
            self.assertEqual(run.call_count, 1)

    @patch.object(generation, "find_assistant", return_value="/existing-assistant")
    def test_timeout_has_no_automatic_retry(self, binary):
        with patch.object(generation.subprocess, "run", side_effect=[
            subprocess.CompletedProcess([], 0, "Logged in using ChatGPT", ""),
            subprocess.TimeoutExpired("assistant", 1),
        ]) as run:
            with self.assertRaisesRegex(ValueError, "timed out"):
                generation.run_assistant("facts", {}, timeout=1)
            self.assertEqual(run.call_count, 2)


if __name__ == "__main__":
    unittest.main()

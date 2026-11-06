# Release readiness — November 6, 2026

## Signing setup follow-up

The user approved a new Apple Distribution identity and ChorePilot App Store
profile. The encrypted signing export, profile, and Firebase configuration are
now GitHub `release-testing` secrets, and the user entered the export password
directly. The environment permits only `main`, requires ErickSorto's review,
and disallows administrator bypass. The four secret names are present; use the
[signing-only verification workflow](SIGNING-CHECK.md) to test their usability.

Certificate-to-profile matching adds three regression tests; 79 Python tests
now pass. Store API credentials and release enablement switches remain
unconfigured. No app build or store action has run. The dated inventory below
records the earlier state before this explicitly approved signing setup.

This branch publishes the previously prepared release tooling for review. It is
not an application release candidate. Both manual pipelines remain disabled.

## Verified store and repository state

- Google Play: ChorePilot is in production at version 1.0.2, code 52, with a
  100% rollout. Internal testing still serves code 4 (1.0).
- Play Console reports no unpublished changes and Policy status says
  “No issues found.” Managed publishing is off. Last publication: October 12.
- The developer account separately asks for a trader-status declaration by
  February 2027; no declaration was made during this review.
- The signed-in App Store Connect session shows iOS 1.0 Ready for Distribution,
  using build 7. The highest visible uploaded build is 7, uploaded October 2;
  builds 4–7 are complete. The unrelated macOS 1.0 draft remains untouched.
  The existing iOS version uses automatic release after approval. Future pipeline
  preparation deliberately selects manual release; that setting was not changed.
- An existing active team API key and a matching local private-key filename were
  found. No key was created, opened, copied, or tested against the API. A working
  API credential and Apple distribution signing still need verification.
- GitHub's public repository has no release secrets, environments, or variables.
  Its only existing workflow is ChorePilot CI.

## Source reconciliation before a build

The reviewed pipeline files were copied from the local app checkout and their
hashes matched the October installation records. The signing/version/export
additions were transplanted onto current GitHub main without copying unrelated
app edits or changing its fallback version numbers.

The app on GitHub main is materially older than the local app: main defaults to
Android 1.0/code 2 and still requires a production RevenueCat key. The local
checkout defaults to 1.0.2/code 52 and contains the later free-beta implementation,
privacy/account-deletion work, helper-proof changes, invite handoff, and UI work.
It has 498 modified and 963 deleted tracked files, plus 355 untracked entries;
many are generated Pods or output artifacts. Do not publish that entire directory.

A separate local `release/app-candidate` branch now reconciles the app source,
including required untracked source/resources and the current-main navigation
fix. It is unpushed and separate from this tooling PR. Its 384 Android unit tests
and shared iOS simulator Kotlin compilation passed; a signed native build and
device checks remain. Do not disable the RevenueCat guard on old main merely to
make a build pass. Historical INVENTORY/VALIDATION documents describe the local
October checkout, not a verification of the app on this branch.

Known behavior to verify includes iPhone invitation handoff and submission of
proof for someone else's chore. The firebaseapp.com invite redirect intentionally
opens a browser landing page; its Open button targets the associated web.app
domain. The complete signed-device handoff still needs verification. Historical production
proof-upload 403s were associated with a deployed/local backend mismatch.
No backend deployment or fresh production-function comparison occurred here.

## Remaining activation inputs

1. Confirm the intended source, prior shipped source baseline, and release
   version. Proposed 1.0.3 / Android 53 / iOS 8 remain provisional; no real new
   release manifest is approved. Browser login and current build inventory are
   now verified; highest observed Android/iOS builds are 52/7 respectively.
2. Select and verify the existing Android upload key and passwords, Apple signing
   certificate/profile, and store API credentials. Firebase files and a local
   Android keystore exist; no release credential environment variables are set.
3. Authorize storing the exact credentials in the GitHub release environments,
   configure their branch/reviewer restrictions, and set enablement variables.
   No credentials were exported, created, broadened, or stored by this review.
4. Review notes/translations, select the tested app commit, and verify current
   factual store declarations. Enable/confirm Managed publishing before using
   this pipeline's Android production submission path. No store settings changed.

Use `bash scripts/check-release-tooling.sh` with Ruby 3.4 and the pinned bundle
to validate the release tooling. Ruby test files run separately because the
stubbed Fastlane tests and real Spaceship tests have incompatible process globals.

## Added generation in the existing flow

`generate-notes VERSION --base VERIFIED_PREVIOUS_RELEASE_COMMIT` now drafts
and translates all configured languages through the existing ChatGPT CLI login.
It updates the same release manifest and review preview, clears review hashes,
and binds generated notes to the committed source and release configuration.
Source changes, uncertain facts, invalid locales/lengths, and concurrent input
changes stop the flow. No second manifest, credential, paid API service, or
unattended hosted AI workflow was introduced. See [README-notes.md](README-notes.md).

The explicit baseline is important for this app: the broad local source snapshot
cannot establish exactly what changed since the shipped binaries. Reviewed facts
can clarify commit subjects, but cannot substitute for a verified baseline.

## Validation in this review

- 76 Python release-tool tests passed, including 16 generation tests.
- 65 Ruby release-tool tests / 355 assertions passed using the pinned bundle.
- Gradle configuration (`help`) passed on the isolated current-main branch.
  This is not a signed build or a device test; existing KMP/AGP deprecation
  warnings remain.
- The StoreRelease scheme references the correct existing app target.
- A live synthetic fixture generated valid English, Spanish, French, German,
  Portuguese, and Russian drafts using the existing ChatGPT login. The preview
  was created, approvals remained empty, and reviewed export was blocked.
  No real ChorePilot facts or store operations were involved in that smoke test.
- Separately, 148 backend unit tests passed in the original local app checkout.
  These do not prove the deployed backend matches that source.
- No signed AAB/IPA, test upload, backend deployment, review submission, public
  release, merge, or hosted build was performed.

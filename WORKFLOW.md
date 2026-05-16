---
tracker:
  kind: linear
  api_key: $LINEAR_API_KEY
  # Replace this with the slug from your Linear project URL.
  # Example URL: https://linear.app/acme/project/chorepilot-a1b2c3d4e5f6
  # Slug: chorepilot-a1b2c3d4e5f6
  project_slug: "chorepilot-kmm-app-eb2390df7ac3"
  active_states:
    - Todo
    - In Progress
    - Rework
  terminal_states:
    - Done
    - Closed
    - Cancelled
    - Canceled
    - Duplicate
polling:
  interval_ms: 30000
workspace:
  root: ~/code/chorepilot-symphony-workspaces
hooks:
  after_create: |
    git clone --depth 1 https://github.com/ErickSorto/ChorePilot.git .
    xattr -dr com.apple.provenance . 2>/dev/null || true
    chflags -R nouchg,noschg . 2>/dev/null || true
    chmod +x ./gradlew 2>/dev/null || true
    git config --global --add safe.directory "$PWD" 2>/dev/null || true
agent:
  max_concurrent_agents: 1
  max_turns: 20
codex:
  command: PATH=/Users/ericksorto/.local/share/fnm/node-versions/v24.15.0/installation/bin:$PATH /Users/ericksorto/.local/share/fnm/node-versions/v24.15.0/installation/bin/codex --config shell_environment_policy.inherit=all app-server
  approval_policy: never
  thread_sandbox: danger-full-access
  turn_sandbox_policy:
    type: danger-full-access
---
You are working on Linear issue `{{ issue.identifier }}` for the ChorePilot repository.

Issue title: {{ issue.title }}
Issue status: {{ issue.state }}
Issue labels: {{ issue.labels }}
Issue URL: {{ issue.url }}

Issue description:
{% if issue.description %}
{{ issue.description }}
{% else %}
No description provided.
{% endif %}

## Project Context

This repository is Kotlin Multiplatform / Compose Multiplatform. Shared Kotlin code lives in `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot`.

Follow the repository's `AGENTS.md` instructions exactly:
- Prefer shared UI, theme, and presentation logic in `commonMain` unless platform APIs are required.
- Keep feature code under `features/<feature>` with `data`, `domain`, and `presentation` folders.
- Keep feature screens dumb: immutable state, an `onEvent` lambda, and explicit navigation lambdas.
- Create ViewModels inside navigation destinations, not inside screen composables.
- Never pass a `NavController` into a feature screen.
- Register shared dependencies in Koin modules.
- Add or update previews for new or meaningfully changed UI.

## Operating Rules

1. Work only inside the workspace Symphony created for this issue.
2. Immediately create or update one Linear progress comment named `## Codex Workpad` before doing substantial work. Include: current interpretation, tight plan, files likely to change, validation target, and any uncertainty.
3. Start by reading `AGENTS.md`, the issue description, and only the relevant feature files. Read navigation, DI, theme, and strong UI examples only when they are relevant to the issue.
4. Create a branch for the issue from the latest `origin/main`.
5. Keep the same `## Codex Workpad` comment updated after each meaningful step and before any long validation command. The user should be able to tell what is being solved from Linear without opening logs.
6. Make focused, production-quality changes that satisfy the issue rather than broad refactors.
7. Prefer existing repository patterns and design-system components over inventing new ones.
8. Run targeted validation for the changed area. For broad shared changes, run the most relevant Gradle checks you can complete in the workspace.
9. Commit the finished work, push the branch, and open a GitHub pull request when credentials allow it.
10. Move the Linear issue to `Human Review` only after validation passes and the PR is linked. If your Linear team uses a different review status, use that configured handoff status instead.
11. If blocked by missing auth, secrets, unavailable tools, hidden confirmation prompts, or an ambiguous product requirement, update the workpad with the exact blocker and stop cleanly. Do not wait indefinitely for user input that Symphony cannot surface.

## Focus Rules

- For FeedbackPal-created issues, treat `## Original message`, `## Problem`, and `## Next step` as the source of truth.
- Keep the first pass narrow. If the issue is a small bug or polish request, fix the smallest complete path first, then validate.
- Do not run broad audits, generate large visual explorations, or inspect unrelated `output/`, `tmp/`, build, `.gradle`, or `Pods` content unless the issue directly requires it.
- Do not browse the web unless the issue depends on external, current documentation. Prefer local code and repository patterns.
- If a UI issue mentions keyboard/IME, bottom navigation, or overlays, inspect the relevant Compose container and apply platform-safe inset handling before redesigning the screen.

## Validation Hints

Use the narrowest command that proves the change. Common starting points:

```sh
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:allTests
```

For UI work, inspect previews or screenshots when available and verify light and dark mode behavior.

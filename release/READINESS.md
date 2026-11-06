# Release readiness — November 6, 2026

This branch publishes the previously prepared release tooling for review. It is
not an application release candidate. Both manual pipelines remain disabled.

## Verified store and repository state

- Google Play: ChorePilot is in production at version 1.0.2, code 52, with a
  100% rollout. Internal testing still serves code 4 (1.0).
- Play Console reports no unpublished changes and Policy status says
  “No issues found.” Managed publishing is off. Last publication: October 12.
- The developer account separately asks for a trader-status declaration by
  February 2027; no declaration was made during this review.
- App Store Connect requires sign-in, so its current version, build inventory,
  and review status remain unverified. The local October 2 upload record confirms
  version 1.0 build 7 was uploaded, but not submitted in that operation.
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

Before activating, reconcile the intended app source into a dedicated release
branch, include required untracked source/resources, and run Android/iOS build
and device checks. Do not disable the RevenueCat guard on old main merely to
make a build pass. Historical INVENTORY/VALIDATION documents describe the local
October checkout, not a verification of the app on this branch.

Known behavior to verify includes iPhone invitation handoff and submission of
proof for someone else's chore. The local invite redirect uses firebaseapp.com,
while the inspected iOS associated domains include web.app and chorepilot.app;
the signed-device handoff needs explicit verification. Historical production
proof-upload 403s were associated with a deployed/local backend mismatch.
No backend deployment or fresh production-function comparison occurred here.

## Remaining activation inputs

1. Sign in to App Store Connect and verify all uploaded build numbers. Proposed
   1.0.3 / Android 53 / iOS 8 remain provisional; no release manifest is approved.
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

## Validation in this review

- 60 Python release-tool tests passed.
- 65 Ruby release-tool tests / 355 assertions passed using the pinned bundle.
- Gradle configuration (`help`) passed on the isolated current-main branch.
  This is not a signed build or a device test; existing KMP/AGP deprecation
  warnings remain.
- The StoreRelease scheme references the correct existing app target.
- Separately, 148 backend unit tests passed in the original local app checkout.
  These do not prove the deployed backend matches that source.
- No signed AAB/IPA, test upload, backend deployment, review submission, public
  release, merge, or hosted build was performed.

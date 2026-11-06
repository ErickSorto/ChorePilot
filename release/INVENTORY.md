# ChorePilot release inventory — 2026-10-20

- Checkout: `/Users/ericksorto/AndroidStudioProjects/ChorePilot`
- Remote: `https://github.com/ErickSorto/ChorePilot.git`
- Branch inspected: `codex/friendly-feedback-report-copy`, HEAD `4ec423861`
- Pre-existing tracked changes: 497 modified, 963 deleted, plus untracked files.
- Android module `:composeApp`, package `com.sortoapps.chorepilot`; local version `1.0.2` / `52`.
- iOS target `iosApp`, workspace `iosApp/iosApp.xcworkspace`; bundle `com.sortoapps.chorepilot`, team `9J5J9J3D9R`; local version `1.0` / `7`.
- Toolchain: Gradle 9.7.0, AGP 9.2.1, Kotlin 2.4.10, SDK 37, Java 17, CocoaPods 1.16.2; iOS deployment target 18.2.
- Existing `.github/workflows/ci.yml` stays intact. It runs Gradle/Functions checks, not store distribution. Existing Firebase test scripts remain separate.

## Changes made

Added a manual, disabled-by-default store pipeline and shared `StoreRelease`
scheme. Android now supports an explicit environment-backed store signing config
and manifest version overrides. The existing debug/internal Firebase variants
retain their behavior. The bundle export depends on `bundleRelease`, preserving
the existing navigation/R8 verification. Release tooling does not enable paid
features or require RevenueCat keys; this app is currently the free family beta.

## Store text provenance

Copied existing draft text verbatim from
`output/store-listing-pilot/{english,portuguese-brazil,spanish,german,french,russian}`
into `release/metadata/{android,ios}`. Hyphenated filenames were normalized to
fastlane names; `privacy-policy-url.txt` becomes `privacy_url.txt`. These 66 files
include titles, descriptions and iOS URLs/keywords; screenshots were not copied.
Migration does not certify translations, claims, URLs, legal text, artwork or
store approval. Review against the current app before production use.

Canonical locales: en-US, pt-BR, es-ES, de-DE, fr-FR, ru-RU. Apple uses `ru` for
Russian. Adjust configured store locales after verifying the active listings.

The local Firebase files and the localization generator
`scripts/generate-localization.py` / `scripts/localization-plural-suffixes.json`
are currently untracked or ignored. The latter scripts and required source
resources must be included in the intended published app branch.

## Before first use

The release branch must include the intended current app sources and all required
untracked build scripts/resources. The new pipeline cannot reproduce local
uncommitted changes from a remote checkout. No unrelated edits were staged,
committed, reset, or selected for publication during pipeline setup.

Choose new store versions from the consoles. The values below are local evidence,
not a fresh verification of live store state. No app release was initialized or
approved on your behalf. Read [SETUP.md](SETUP.md) for authorization and credentials.

Current Gradle configuration checks pass with the existing legacy AGP/KMP flags.
Gradle warns that the combined Android application/KMP module uses deprecated
compatibility and will need migration before AGP 10; this pipeline does not change
the application architecture or dependency versions.

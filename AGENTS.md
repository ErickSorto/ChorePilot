# ChorePilot Agent Guide

## Architecture
- This project is Kotlin Multiplatform / Compose Multiplatform. Prefer shared UI, theme, and reusable presentation logic in `commonMain` unless a platform-specific API is required.
- Shared Kotlin code lives in `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot`.
- Keep features under `features/<feature>` using lowercase directory names.
- Every feature keeps `data`, `domain`, and `presentation` folders, even when `data` or `domain` only contain placeholders.

## Naming
- Packages are lowercase: `com.sortoapps.chorepilot`.
- Public Kotlin types and files use PascalCase.
- Route definitions live in `navigation/AppRoute.kt`.
- Screen state types are named `[Feature]UiState`.
- Screen event types are named `[Feature]UiEvent`.
- ViewModels are named `[Feature]ViewModel`.
- Screen composables are named `[Feature]Screen`.

## Navigation
- Root navigation lives in [`composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/navigation/ChorePilotNavHost.kt`](/Users/ericksorto/AndroidStudioProjects/ChorePilot/composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/navigation/ChorePilotNavHost.kt).
- Bottom navigation lives in [`composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/navigation/MainScreen.kt`](/Users/ericksorto/AndroidStudioProjects/ChorePilot/composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/navigation/MainScreen.kt).
- Use typed navigation routes with `@Serializable`.
- Create feature ViewModels inside each `composable<...>` destination and collect state there with `collectAsStateWithLifecycle()`.
- Never create a ViewModel inside a screen composable.
- Never pass a `NavController` into a feature screen.

## UI Contract
- Screen composables stay dumb: they receive immutable state, an `onEvent` lambda, and explicit navigation lambdas.
- ViewModels own state mutation and expose `StateFlow`.
- Prefer placeholder UI that compiles and demonstrates flow over TODO-only files.
- Route/container composables should stay separate from pure presentational content where the split improves previewing, testing, or adaptive layout work.
- Prefer reusable composables and shared building blocks over large monolithic screen files.

## UI Quality Bar
- UI must feel premium, polished, and production-ready. Do not ship default, generic, starter-app Compose output.
- Inspect the existing design system, theme, preview helpers, and strongest current screens before adding new UI. Start with `core/designsystem`, `core/preview`, and the best-looking feature screens.
- Reuse existing good patterns before inventing new ones. Extend the system deliberately when the repo is missing a needed primitive.
- Aim for stronger hierarchy, cleaner CTA emphasis, better spacing rhythm, clearer grouping, and more intentional surface treatment.
- Avoid random spacing, weak hierarchy, flat repetitive card stacks, and "text/buttons in a Column" layouts.
- Shared UI should stay in `commonMain` where reasonable, including reusable components, preview shells, and theme-aware building blocks.
- Prefer Compose Multiplatform shared resources for strings, fonts, and images when it fits the existing repo cleanly.
- Support dark mode, larger layouts, and adaptive behavior for any new or meaningfully refactored UI.
- Use consistent spacing, typography, and radii. Favor theme tokens or clear local constants over scattered magic numbers.
- Loading, content, empty, and error states should be handled when the feature meaningfully needs them, even if some states are preview-only at first.
- New or refactored UI must include previews. Add representative light/dark and alternate-size previews whenever practical.
- Use the existing preview and HotSwan setup for validation when possible; polish work is not complete until it has been previewed.

## UI Task Workflow
- Execute UI tasks with an explicit `Goal`, `Context`, `Constraints`, and `Done When` framing, even if this only appears in a short internal plan.
- Before coding, inspect the relevant screen plus the repo's best existing UI examples to match the app's quality ceiling.
- Extract reusable parts when the same layout, section, or surface treatment is likely to recur.
- Keep presentational composables previewable with stable sample data.

## UI Done When
- The result looks intentional, not default.
- Hierarchy is immediately scannable on phone and larger widths.
- Spacing, type, and radii feel consistent with the surrounding app.
- Surfaces and section breaks help comprehension instead of adding noise.
- Dark mode remains polished and readable.
- Relevant empty/loading/error variants are accounted for.
- Previews exist and are useful for future iteration.

## Dependency Injection
- Common Koin bootstrapping lives in [`composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/di/Koin.kt`](/Users/ericksorto/AndroidStudioProjects/ChorePilot/composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/di/Koin.kt).
- Register shared bindings in `appModule` and ViewModels in `viewModelModule`.
- Keep `platformModule` actuals in `androidMain` and `iosMain`.
- Use `viewModelOf(::FeatureViewModel)` when the constructor shape allows it.

## Adding Features
- Add a new folder at `features/<feature>`.
- Create `presentation/screen/[Feature]Screen.kt`.
- Create `presentation/viewmodel/[Feature]UiState.kt`, `[Feature]UiEvent.kt`, and `[Feature]ViewModel.kt`.
- Add `.gitkeep` files for any intentionally empty `data`, `domain`, or `components` directories.
- Register the ViewModel in `viewModelModule`.
- Add typed routes and nav destinations in the `navigation` package, not inside the feature folder.

## Gradle
- Keep shared dependency versions in `gradle/libs.versions.toml`.
- Preserve the Compose/KMP plugin alias style already used in `composeApp/build.gradle.kts`.
- Add only architecture dependencies needed for the current feature set.

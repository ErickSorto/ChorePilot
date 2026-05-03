---
name: compose-ui-premium
description: Use when refining Compose Multiplatform screens, shared UI components, previews, adaptive layouts, or design-system polish in ChorePilot. This skill is repo-specific and points to exact screens, components, and code patterns to copy from.
---

# Compose UI Premium Skill

Read these first, in order:
1. `AGENTS.md`
2. `design/ui-reference.md`
3. `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/core/designsystem`
4. `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/core/preview`

This skill is for ChorePilot-specific Compose UI work. It should push future work toward the app's best existing quality bar, not toward default Material demo output.

## Mission

Design, evaluate, and implement UI so first-time users can understand what the screen is for quickly, while returning users can scan state and act without friction.

This skill is authoritative for:
- screen-level hierarchy and orientation
- interaction clarity and progressive disclosure
- visual consistency across containers, controls, and states
- preview/evidence-based validation for Compose UI work

## Product Mindset

- Act like a principal product designer and implementation partner, not a passive stylist.
- Prioritize comprehension, trust, and actionability over novelty.
- Treat user confusion as a design failure, not a copy problem to patch later.
- Optimize for real household coordination use under time pressure, not just static visual appeal.

## When To Use

Use this skill when a task involves:
- refining a screen, section, list, card, form, or flow
- adding new shared UI in `commonMain`
- improving hierarchy, spacing, or dark mode polish
- extracting reusable composables from a large screen
- adding previews or adaptive variants

## Non-Negotiables

- Inspect the current repo before coding.
- Open the referenced source material before coding. Do not rely on the skill text alone when links are provided.
- Reuse the strongest existing ChorePilot patterns before inventing new ones.
- Prioritize clarity over sophistication.
- Make the primary purpose of the screen obvious in one glance.
- Use progressive disclosure by default when advanced detail exists.
- Keep state explicit: loading, empty, blocked, done, waiting, or actionable should never be ambiguous.
- Audit consistency, not just beauty: container sizing, border strength, elevation/shadow use, accent direction, and control styling should feel like one system.
- Keep route/container logic separate from previewable presentational UI when it improves reuse.
- Add previews for any new or refactored UI.
- Visually verify meaningful UI changes when runnable previews or screens exist.
- Do not finish until the UI reads as intentional and product-grade.

## Core Design Rules

Always:
- orient the user immediately: what this surface is, why it matters, and what they can do next
- teach before exposing complexity
- let one primary action or focal state dominate
- hide empty, unknown, or non-applicable noise when it does not help decision-making
- prefer signal over decoration for color, motion, borders, and icons
- explain action consequence before commit when the action meaning is not obvious

## ChorePilot Modernity Gate

Every meaningful UI pass should improve these:

1. Primary intent is obvious in 3 seconds.
2. One primary action or focal state clearly dominates.
3. Progressive disclosure is used for secondary detail.
4. State is explicit, not implied.
5. The first screenful tells the user where to look.
6. Controls feel integrated into the composition, not dropped in.
7. Color and icon use communicates meaning instead of adding decoration.
8. Removing 20-30% of the chrome would not harm understanding.

If the screen fails several of these, it needs a structural redesign, not just polish.

## Comprehension Bar

Every major surface should pass:
- `3-Second Check`: a first-time user can tell what the screen is about immediately
- `5-Second Check`: a non-technical person could explain what is happening and what to do next

If either fails, treat it as a high-priority UX fix.

## Progressive Persona Ladder

Every meaningful surface should serve all three levels:
1. new or casual user: understands purpose and next step
2. regular household user: can scan status and make the common decision quickly
3. power/admin user: can inspect enough detail to validate or override without friction

If a surface only works for the power user, it is not finished.

## Goal / Context / Constraints / Done When

Before coding, write a short internal plan with:

- Goal: what the UI needs to help the user do
- Context: which existing screen or pattern this should feel adjacent to
- Constraints: shared `commonMain`, current theme, navigation, preview setup, feature state shape
- Done When: how you will know the result is better than default Compose output

Also identify:
- the primary user decision on that screen
- what should be visible immediately vs progressively disclosed
- at least one comprehension problem or inconsistency to remove

Before the plan is considered complete:
- Open the local repo references listed below.
- Open at least one relevant external reference link when the task is a meaningful UI polish/refactor, unless network access is unavailable.
- If you skip any linked reference, say why.
- Identify at least one concrete inconsistency to remove, not just one surface to beautify.

## Exact Repo References To Inspect

Use these files as the primary source material for future work.

These are required reads, not optional suggestions, for non-trivial UI work.

### 1. Header and top-of-screen rhythm

Inspect:
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/features/home/presentation/components/HomeHeaderComponents.kt`

Copy these ideas:
- strong headline first
- compact, controlled top-row chrome
- horizontally scannable interactive elements
- selected state communicated with both color and weight

Especially useful for:
- dashboards
- section switchers
- date pickers
- profile and household overview headers

### 2. Premium snapshot and progress treatment

Inspect:
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/features/home/presentation/components/HomeSnapshotComponents.kt`

Copy these ideas:
- one focal summary card
- compact progress visualization
- restrained accent usage
- meaningful surface treatment without over-styling

Especially useful for:
- progress summaries
- streaks
- status cards
- "today" or "this week" highlights

### 3. Best current premium screen composition

Inspect:
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/features/reports/presentation/screen/ReportsScreen.kt`

This is the strongest reference for:
- control rows
- grouped sections
- section cards
- premium-but-utility-focused screen composition

Copy these ideas:
- start with high-value controls at the top
- use grouped surfaces instead of endless flat lists
- emphasize one key metric area before secondary content
- use soft borders and subtle alpha surfaces instead of loud card stacks

### 4. Preview scaffolding

Inspect:
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/core/preview/PreviewShell.kt`
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/core/preview/ChorePilotPreviewData.kt`

Copy these ideas:
- preview content inside the app shell where useful
- keep fake chrome realistic enough to judge hierarchy
- prefer preview data factories over ad hoc literals when possible

## External Reference Set

Use these real external examples as additional reference material. Do not copy branding, app concept, or file structure. Copy the quality of composition and interaction patterns.

For meaningful UI polish work, you must open the relevant external reference pages or source files before implementing. Reading the summary in this skill is not enough on its own.

### Crane: official Compose sample for the Material study

Primary sources:
- `https://material.io/design/material-studies/crane.html`
- `https://github.com/android/compose-samples/tree/e6994123804b976083fa937d3f5bf926da4facc5/Crane`
- `https://raw.githubusercontent.com/android/compose-samples/e6994123804b976083fa937d3f5bf926da4facc5/Crane/app/src/main/java/androidx/compose/samples/crane/home/LandingScreen.kt`
- `https://raw.githubusercontent.com/android/compose-samples/e6994123804b976083fa937d3f5bf926da4facc5/Crane/app/src/main/java/androidx/compose/samples/crane/home/CraneHome.kt`
- `https://raw.githubusercontent.com/android/compose-samples/e6994123804b976083fa937d3f5bf926da4facc5/Crane/app/src/main/java/androidx/compose/samples/crane/base/CraneTabs.kt`
- `https://raw.githubusercontent.com/android/compose-samples/e6994123804b976083fa937d3f5bf926da4facc5/Crane/app/src/main/java/androidx/compose/samples/crane/home/SearchUserInput.kt`

Concrete examples to borrow:
- `LandingScreen.kt`: a dedicated landing moment that is short, branded, and transitions cleanly into the real app instead of feeling like a static splash.
- `CraneHome.kt`: `BackdropScaffold` separates search criteria from exploration content, giving the screen layered depth and stronger rhythm than a single flat list.
- `CraneTabs.kt`: tabs are made visually distinct with explicit selected outlines instead of relying on default indicator styling.
- `SearchUserInput.kt`: user inputs are presented as tappable structured rows with icons and captions, which makes utility inputs feel premium and scannable.

Translate into ChorePilot:
- use layered surfaces when there is a real “controls vs results” relationship
- use branded or expressive screen entry moments sparingly, mainly for onboarding or special transitions
- style tabs and selectors as part of the composition, not as untouched defaults
- prefer structured tappable input rows for chores, family, and scheduling flows

Do not copy literally:
- do not introduce travel-specific tab metaphors
- do not add layered scaffolds where a normal screen would be clearer

### Wiggles: expressive card-led Compose presentation

Primary sources:
- `https://github.com/Spikeysanju/Wiggles`
- `https://raw.githubusercontent.com/Spikeysanju/Wiggles/main/README.md`
- `https://raw.githubusercontent.com/Spikeysanju/Wiggles/main/app/src/main/java/dev/spikeysanju/wiggles/component/TopBar.kt`
- `https://raw.githubusercontent.com/Spikeysanju/Wiggles/main/app/src/main/java/dev/spikeysanju/wiggles/component/ItemDogCard.kt`
- `https://raw.githubusercontent.com/Spikeysanju/Wiggles/main/app/src/main/java/dev/spikeysanju/wiggles/view/Details.kt`
- `https://raw.githubusercontent.com/Spikeysanju/Wiggles/main/app/src/main/java/dev/spikeysanju/wiggles/ui/theme/Theme.kt`

Concrete examples to borrow:
- `README.md` screenshots: home and detail screens use a strong image-led focal area and clear section spacing, especially in dark mode.
- `TopBar.kt`: the top area is not just an app bar; it combines greeting, supporting line, and a single trailing action so the first viewport feels more personal and composed.
- `ItemDogCard.kt`: cards mix thumbnail, textual hierarchy, metadata row, and a small status tag so each row feels designed instead of purely utilitarian.
- `Details.kt`: the details screen opens with a full-bleed image, then moves into titled sections and ends with one strong CTA.
- `Theme.kt`: a custom palette and fonts help the UI feel authored instead of stock.

Translate into ChorePilot:
- use one strong focal card or image-led section when a feature benefits from emotional weight
- treat smaller list items as mini compositions with hierarchy, not just label-value rows
- allow top-of-screen copy to carry mood and orientation together
- use custom type and color intentionally, but inside the existing ChorePilot system

Do not copy literally:
- do not copy Wiggles’ older Material 2 APIs or exact card styling
- note that `Home.kt` has a list rendering bug pattern; do not copy that implementation shape, only the presentation ideas

### Driftly: polished Material 3 utility-app execution

Primary sources:
- `https://github.com/DP-Hridayan/Driftly`
- `https://raw.githubusercontent.com/DP-Hridayan/Driftly/master/README.md`
- `https://raw.githubusercontent.com/DP-Hridayan/Driftly/master/app/src/main/java/in/hridayan/driftly/home/presentation/screens/HomeScreen.kt`
- `https://raw.githubusercontent.com/DP-Hridayan/Driftly/master/app/src/main/java/in/hridayan/driftly/home/presentation/components/card/SubjectCard.kt`
- `https://raw.githubusercontent.com/DP-Hridayan/Driftly/master/app/src/main/java/in/hridayan/driftly/calender/presentation/components/card/AttendanceCardWithTabs.kt`
- `https://raw.githubusercontent.com/DP-Hridayan/Driftly/master/app/src/main/java/in/hridayan/driftly/core/presentation/components/card/PillShapedCard.kt`
- `https://raw.githubusercontent.com/DP-Hridayan/Driftly/master/app/src/main/java/in/hridayan/driftly/core/presentation/theme/Theme.kt`

Concrete examples to borrow:
- `README.md` screenshots and description: the app keeps a utility workflow polished with strong Material 3 structure and good dark-mode treatment.
- `HomeScreen.kt`: the home screen pairs a strong title row, an expressive progress area, and a floating action button without losing operational clarity.
- `SubjectCard.kt`: cards support multiple styles and utility actions while still feeling cohesive and animated.
- `AttendanceCardWithTabs.kt`: tabs, pager, progress graphic, and labels are combined into one coherent card rather than split into unrelated components.
- `PillShapedCard.kt`: shape is derived from measured height, which is a good reminder that reusable components can encode more deliberate geometry.
- `Theme.kt`: dynamic color, dark mode, and higher-contrast modes are handled as first-class design concerns.

Translate into ChorePilot:
- keep utility screens polished without turning them into marketing layouts
- use one composed card or control cluster to bind related functionality together
- treat dark mode and adaptive theme choices as part of the design system, not as a later patch
- allow reusable shared components to encode stronger visual character when that character is useful across features

Do not copy literally:
- do not port Driftly’s Android-only APIs or expressive theme setup directly into KMP code
- use the quality of grouping and surface treatment, not the exact attendance-domain components

## Use These Patterns Directly

These snippets are adapted from the repo and should be used as starting points.

### A. Premium screen skeleton

Use this when building a new screen or major section:

```kotlin
@Composable
fun FeatureContent(
    state: FeatureUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FeatureHero(
                    title = state.title,
                    supportingText = state.supportingText,
                )
            }

            item {
                FeatureControls(
                    selectedFilter = state.selectedFilter,
                    onSelect = {},
                )
            }

            item {
                FeaturePrimarySection(state = state)
            }

            item {
                FeatureSecondarySection(state = state)
            }
        }
    }
}
```

Why this works in ChorePilot:
- the screen opens with hierarchy
- controls sit above content instead of floating randomly
- sections are grouped with deliberate rhythm

### B. Soft premium control row

Use this for tabs, period selectors, filters, or small switchers.
This comes from the `ReportsScreen` control treatment.

```kotlin
@Composable
fun FeatureControlStrip(
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        ),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
```

Use this instead of:
- plain text row + dividers
- bare `SegmentedButtonRow` dropped in with default styling
- multiple unrelated small cards in a row

### C. Focal summary card

Use this when the screen needs one strong "here is the state right now" section.
This pattern should feel close to `DailySnapshotCard` and `WeeklyStatusCard`.

```kotlin
@Composable
fun FeatureSummaryCard(
    title: String,
    summary: String,
    trailing: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            trailing()
        }
    }
}
```

### D. Preview wrapper pattern

Default to this shape for screen previews:

```kotlin
@Preview(name = "Feature Screen", widthDp = 393, heightDp = 852)
@Composable
private fun FeatureScreenPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.Home) { modifier ->
            FeatureScreen(
                state = ChorePilotPreviewData.featureState(),
                onEvent = {},
                modifier = modifier,
            )
        }
    }
}
```

If the real screen does not accept a `modifier`, consider extracting:
- `FeatureRoute(...)`
- `FeatureContent(state, onEvent, modifier = ...)`

Then preview `FeatureContent`.

## External-To-ChorePilot Translation Shortcuts

Use these shortcuts while designing:

- If the task needs layered controls and results, think `CraneHome.kt` first, then simplify for ChorePilot.
- If the task needs a more memorable hero or top section, think `Wiggles TopBar + Details` and adapt that energy to ChorePilot’s calmer tone.
- If the task needs polished utility UI with Material 3 discipline, think `Driftly HomeScreen + AttendanceCardWithTabs`.
- If the task needs a reusable special-shape surface, inspect `Driftly PillShapedCard` and decide whether ChorePilot needs a shared primitive in `core/designsystem`.

## What To Copy From Which Screen

Use this decision guide.

### If the task is a dashboard-like screen

Start with:
- `HomeScreen`
- `HomeHeaderComponents`
- `HomeSnapshotComponents`
- `ReportsScreen`

Copy:
- focal summary area
- grouped sections
- selected control state
- progress visualization

### If the task is an analytics or reporting area

Start with:
- `ReportsScreen`

Copy:
- control rows
- surface grouping
- metric emphasis
- summary plus chart layout

### If the task is a simple utility screen

Do not default to:
- one `LazyColumn`
- repeated `Card { Column { Text(...) } }`

Instead:
- still create one focal top section
- group related rows
- give one element stronger emphasis
- use at least one preview that shows the intended app shell
- check Driftly before coding, because it is the best external reference for polished utility-app composition

### If the task is an onboarding or premium moment

Start with:
- `Crane LandingScreen`
- `Wiggles Details`
- `features/premium`

Copy:
- one strong first impression
- short, deliberate vertical sequence
- one clear CTA
- richer visual rhythm than the normal utility screens

### If the task is a search, planner, or scheduling flow

Start with:
- `CraneHome`
- `SearchUserInput`
- `CraneTabs`

Copy:
- structured tappable inputs
- clearly separated control and result layers
- explicit selected states on tabs and segmented navigation

### If the task is a shared component

Place it in:
- `core/designsystem` if multiple features can reuse it
- `features/<feature>/presentation/components` if it is feature-specific

Before adding a new shared primitive, check:
- `ChorePilotComponents.kt`
- `ChorePilotTopAppBar.kt`
- `HomeSnapshotComponents.kt`

## Anti-Patterns To Reject

Stop and redesign if the result looks like any of these:

- a stack of identical cards with the same visual weight
- a `Column` of text blocks and buttons with no focal section
- random use of `16.dp`, `20.dp`, `24.dp`, `28.dp` without a rhythm
- default Material tabs or segmented controls dropped in with no adaptation
- every section trying to be equally loud
- adding gradients and shadows without improving hierarchy

## Repo-Specific Quality Rules

- Use the existing `MaterialTheme` colors, shapes, and type first.
- Favor soft alpha surfaces, subtle borders, and shape consistency over decoration.
- Premium in ChorePilot means calm and deliberate, not flashy.
- Dark mode should feel designed, not merely inverted.
- The first screenful should tell the user where to look.
- Orientation matters more than flourish. The user should quickly understand the purpose of the surface and the next action.
- Similar containers should share a sizing logic. Avoid one-off chip widths, stray empty space, or containers that feel visually unrelated to nearby controls.
- If controls and content live on the same screen, their border weight, radius family, and elevation strategy should feel intentionally related.
- Use accent color systems consistently. If a ranking or severity system is color-coded, keep the direction coherent rather than mixing unrelated hues unless the meaning truly changes.
- Avoid exposing advanced or secondary information at the same visual weight as the main decision.
- Hide filler copy, placeholder metrics, and non-essential helper text when they dilute the main signal.

## Theme Parity Requirements

These are blocking for meaningful UI work:
- full usability in light and dark themes
- acceptable contrast for text, icons, pills, and borders in both themes
- meaning does not rely on color alone when state is important
- disabled, loading, empty, and selected states remain legible
- theme changes should not break hierarchy or affordance clarity

Do not finish a substantial UI pass if dark mode still feels like an afterthought.

## Iconography Semantics

Icons are semantic signals, not decoration.

Rules:
- use icons when they help the user identify a kind of thing faster than text alone
- do not use the same icon treatment for meaningfully different concepts if that creates ambiguity
- ranked, triage, or status-heavy rows should have recognizable visual anchors
- state meaning must not rely on color alone when an icon or label can make it clearer

Heuristic:
> If a user must read every label to understand what each row or control represents, the UI is doing too little.

## Validation and Evidence

When runnable UI or previews exist, verification is part of the job.

For meaningful UI changes:
- visually inspect previews and/or the running surface
- note any obvious layout, spacing, or hierarchy regressions
- mention what was verified in the final response

Claims that a UI is polished without visual verification are weak claims.

## Validation Checklist

Before finishing, check all of these:

- Did I inspect at least one strong existing screen first?
- Did I actually open the referenced local files instead of relying on the skill summary?
- Did I open at least one relevant external reference link when the task called for substantial UI polish?
- Did I remove any obvious inconsistency in sizing, border strength, or elevation while polishing?
- Did I make the main purpose and next action clearer, not just prettier?
- Did I keep advanced detail progressive instead of equally loud?
- Is there one obvious focal area?
- Are controls styled as part of the composition, not dropped in?
- Does the screen avoid repetitive flat card stacking?
- Are loading, empty, blocked, or success states clear when they matter?
- Are icons and accent colors carrying meaning instead of random decoration?
- Is the result still strong in dark mode?
- Does the content have a real preview?
- If the task was non-trivial, did I add at least one alternate preview state?

## Self-Critique Prompt

Run this self-critique before finishing:

"If I compare this against `HomeSnapshotComponents` and `ReportsScreen`, does it feel like it belongs in the same app? Is the purpose obvious in three seconds? What is still too generic, too loud, or too unclear?"

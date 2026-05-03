# ChorePilot UI Reference

This note defines the visual qualities we want to reuse from the external references without copying their branding, app concepts, or exact layouts.

## Reference Links

- [Material Crane study](https://m2.material.io/design/material-studies/crane.html)
- [Wiggles](https://github.com/Spikeysanju/Wiggles)
- [Driftly](https://github.com/DP-Hridayan/Driftly)

## What To Borrow

### Crane

Borrow:
- expressive hierarchy with a clear focal area
- layered surfaces that feel intentional instead of flat
- premium rhythm between hero, supporting content, and action
- editorial warmth and stronger composition than a standard utility app

Translate to ChorePilot:
- make key household status or task areas read as the main visual anchor
- use headline/support/action groupings instead of evenly weighted stacks
- let a screen open with one strong section before moving into supporting details

### Wiggles

Borrow:
- playful but controlled Compose presentation
- richer section composition and more memorable surfaces
- layouts that feel designed, not auto-generated from components

Translate to ChorePilot:
- use better grouping and asymmetry where it helps scanning
- avoid monotonous card lists with identical weight
- keep small utility screens feeling considered, not thrown together

### Driftly

Borrow:
- polished Material 3 execution
- modern dark theme treatment
- responsive structure and cleaner state presentation

Translate to ChorePilot:
- keep surfaces subtle, readable, and premium in dark mode
- use adaptive spacing and pane behavior for larger widths
- rely on Material 3 structure, but elevate it with better rhythm and surface hierarchy

## ChorePilot Translation Rules

- Build for family coordination, chores, proof, and household clarity, not for travel, pets, or attendance branding.
- The UI should feel calm, capable, and premium.
- The strongest section on a screen should be obvious in the first glance.
- Use section headers, grouped actions, and focused summary areas to improve orientation.
- Prefer a small number of strong surfaces over many identical cards.
- Buttons should feel purposeful; avoid stuffing every action into equal-weight controls.
- Dark mode should keep contrast, depth, and readability without muddy overlays.

## Reuse In This Repo

Inspect these before adding new UI:
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/core/designsystem`
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/core/preview`
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/features/reports`
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/features/premium`
- `composeApp/src/commonMain/kotlin/com/sortoapps/chorepilot/features/home`

## Screen Checklist

- Is there one clear focal section?
- Is the spacing cadence consistent?
- Does the surface treatment improve hierarchy instead of adding noise?
- Does the screen still feel polished in dark mode?
- Is the content previewable in isolation?
- Does the result feel product-grade rather than like sample Compose UI?

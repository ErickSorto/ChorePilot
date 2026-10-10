# README media

The README uses real screenshots of ChorePilot's Compose screens. They were captured from a running Android emulator, not generated or recreated as marketing UI.

## Capture context

| Field | Value |
| --- | --- |
| Date | October 10, 2026 |
| App | ChorePilot Android debug 1.0.2 (52) |
| Emulator | Pixel 9 Pro XL AVD, Android 16 / API 36 |
| Capture size | 1080 × 2400 px, density 420 |
| Appearance | English, light theme, font scale 1.0 |
| Data | Offline sample household: Maple House, Alex, Emma, and Noah |
| Rendering | Existing `StoreListingPreviewActivity`, using real feature screen composables with sample state |
| Capture | Android CLI / `adb exec-out screencap -p` |
| System chrome | Android System UI demo clock set to 9:41; notification icons hidden |
| Export | Lossless WebP at the original screenshot dimensions |

The capture activity belongs to the current development workspace under `composeApp/src/androidDebug/kotlin/com/sortoapps/chorepilot/navigation/StoreListingPreviewActivity.kt`. It is a debug-only screenshot fixture, not a production entry point. It supplies offline screen state and no-op callbacks; these images document the rendered UI, not an end-to-end backend test. The navigation shell is a copy used by that fixture.

The working app contains changes beyond the repository's default branch at capture time. This documentation update publishes screenshots and README assets only; it does not publish those app changes. Reports use a fixed sample week, and room-discovery suggestions are sample results rather than a live AI request.

## Screens

| File | Rendered screen | Scene |
| --- | --- | --- |
| `home.webp` | HomeScreen | `home` |
| `chores.webp` | ChoresScreen | `chores` |
| `pets.webp` | PetsScreen | `pets` |
| `scan.webp` | DiscoveryReviewScreen | `scan` |
| `editor.webp` | ChoreEditorScreen | `editor` |
| `reports.webp` | ReportsScreen | `reports` |

No app content was composited, retouched, or replaced after capture. The logo is the existing ChorePilot iOS app icon, resized to 384 × 384 for README delivery.

## Build provenance

The local capture build completed successfully with `:composeApp:assembleDebug` using JDK 17.

- Source workspace HEAD: `4ec4238` with local development changes.
- README update base: `f3c9031469e68e1409328c276ef95a4e5dfe3f06`.
- Debug APK SHA-256: `afe146b244cc6d78f769b58edabbd9735e68f73b12667d76bc31d81bb1990523`.
- Capture activity SHA-256: `ed6707d9b8c3d6ee1bf0dbb1c7f3088afc7399ea5560131c73000a4ae846ebc5`.

With that development build installed, a scene can be opened with:

```bash
adb shell am start -W -S \
  -n com.sortoapps.chorepilot/com.sortoapps.chorepilot.navigation.StoreListingPreviewActivity \
  --es scene home

# Capture after the screen has finished rendering.
adb exec-out screencap -p > home.png
cwebp -lossless home.png -o home.webp
```

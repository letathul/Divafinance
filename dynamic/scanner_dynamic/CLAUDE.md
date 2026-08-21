# dynamic/scanner_dynamic

**Purpose:** Android on-demand delivery wrapper for the receipt scanner. Exists so the ML
Kit text-recognition model isn't in the base APK. One Activity, no logic.

**Gradle:** `:dynamic:scanner_dynamic` · `diva.android.dynamic-feature` · namespace
`com.divafinance.dynamic.scanner`. Registered in `composeApp`'s `android.dynamicFeatures`.
**Depends on:** `:composeApp`, `:core:ui`, `:feature:scanner`
**Android only** — has no iOS or JVM counterpart.

## Key files

| File | What it does |
|------|--------------|
| `src/main/kotlin/com/divafinance/dynamic/scanner/ScannerDynamicActivity.kt` | Hosts `ScannerScreen` from `:feature:scanner` |
| `src/androidMain/AndroidManifest.xml` | `<dist:module>` with `on-demand` delivery, `instant=false`, `fusing include=true`. Activity declared `exported=false`. |

## Conventions / gotchas

- **Sources live in `src/main/kotlin/`, not `src/androidMain/`** — but the manifest is in
  `src/androidMain/`. Match that split when adding files.
- The Compose stack comes from the `diva.android.dynamic-feature` convention plugin;
  nothing is inherited transitively from `:composeApp`.
- OCR can be absent at runtime, which is why `:feature:scanner`'s `OcrEngine` exposes
  `isAvailable()` and the CSV-import tab works without this module.
- Installation is driven by `DynamicFeatureLoader` in `:composeApp`.
- Keep this a thin Activity shell — scanning logic belongs in `:feature:scanner`.

## Tests

None.

```bash
./gradlew :dynamic:scanner_dynamic:assembleDebug
```

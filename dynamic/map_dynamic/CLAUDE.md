# dynamic/map_dynamic

**Purpose:** Android on-demand delivery wrapper for the spending map. Exists so the Google
Maps SDK isn't in the base APK. Contains one Activity and no logic.

**Gradle:** `:dynamic:map_dynamic` · `diva.android.dynamic-feature` · namespace
`com.divafinance.dynamic.map`. Registered in `composeApp`'s `android.dynamicFeatures`.
**Depends on:** `:composeApp`, `:core:ui`, `:feature:map`
**Android only** — has no iOS or JVM counterpart.

## Key files

| File | What it does |
|------|--------------|
| `src/main/kotlin/com/divafinance/dynamic/map/MapDynamicActivity.kt` | Hosts `SpendingMapScreen` from `:feature:map` |
| `src/androidMain/AndroidManifest.xml` | `<dist:module>` with `on-demand` delivery, `instant=false`, `fusing include=true`. Declares the Activity as `exported=false`. |

## Conventions / gotchas

- **Sources live in `src/main/kotlin/`, not `src/androidMain/`** — but the manifest is in
  `src/androidMain/`. That split is unusual and intentional; match it when adding files.
- Nothing is inherited transitively from `:composeApp`, so the Compose stack is declared
  by the `diva.android.dynamic-feature` convention plugin.
- `fusing include=true` means this module is fused into pre-Lollipop-style multi-APK
  builds. The map feature can still be absent at runtime, which is why
  `:feature:map` ships `MapFallbackScreen`.
- Installation is driven by `DynamicFeatureLoader` in `:composeApp`, not from here.
- Keep this module a thin Activity shell — all map logic belongs in `:feature:map` so the
  iOS and desktop targets can use it.

## Tests

None.

```bash
./gradlew :dynamic:map_dynamic:assembleDebug
```

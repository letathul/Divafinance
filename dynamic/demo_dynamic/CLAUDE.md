# dynamic/demo_dynamic

**Purpose:** Android on-demand delivery for the demo guided tour. One Activity, no logic.

**Gradle:** `:dynamic:demo_dynamic` · `diva.android.dynamic-feature` · namespace
`com.divafinance.dynamic.demo`. Registered in `composeApp`'s `android.dynamicFeatures`.
**Depends on:** `:composeApp`, `:core:ui`, `:feature:demo`
**Android only.**

## Key files

| File | What it does |
|------|--------------|
| `src/androidMain/kotlin/com/divafinance/dynamic/demo/DemoDynamicActivity.kt` | Hosts `DemoTourScreen` from `:feature:demo` |
| `src/androidMain/AndroidManifest.xml` | `<dist:module>` on-demand, `instant=false`, and **`fusing include="false"`** |

## Conventions / gotchas

- **`fusing include="false"` — unlike the other three dynamic modules.** The demo must be
  genuinely absent for users who never take the offer, and removable for those who do.
  Don't "fix" this to match the others.
- **This module carries only the tour screen.** The demo *data* lives in `:feature:demo`
  in the base app, so `DemoDataManager.seed()` works even when the split never installs —
  a sideloaded debug build or an offline device still gets a working demo. Install failure
  is best-effort and must not block seeding.
- Install/uninstall is driven through the `DemoModuleInstaller` interface in
  `:feature:demo`, implemented by `PlayDemoModuleInstaller` in `:composeApp`'s
  `di/DynamicFeatureModule.kt`. Uninstall is how "remove demo data" reclaims the split.
- Unlike the other three dynamic modules, this one's Kotlin sources are under
  `src/androidMain/kotlin/` rather than `src/main/kotlin/`. Both work; follow whichever
  the module already uses.

## Tests

None.

```bash
./gradlew :dynamic:demo_dynamic:assembleDebug
```

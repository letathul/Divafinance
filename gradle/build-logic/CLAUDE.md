# gradle/build-logic

**Purpose:** Convention plugins that every module applies instead of repeating KMP,
Android, and Compose configuration in 27 `build.gradle.kts` files. Included via
`includeBuild("gradle/build-logic")` in the root `settings.gradle.kts`.

**Gradle:** composite build, not a project module. Plugins are precompiled script plugins
under `convention/src/main/kotlin/`.

## Key files

| File | What it does |
|------|--------------|
| `convention/src/main/kotlin/diva.kmp.library.gradle.kts` | Base KMP library: toolchain 17, `androidTarget`, `jvm`, three iOS targets, the `jvmShared` source-set group, and the `kotlin.time.ExperimentalTime` opt-in. Applied by all non-Compose `core/*` modules and `:server`. |
| `convention/src/main/kotlin/diva.kmp.compose.gradle.kts` | Extends `diva.kmp.library` with the Compose Multiplatform dependency stack and the `jvmTest` UI-test setup. Applied by `core/ui` and all 13 `feature/*` modules. |
| `convention/src/main/kotlin/diva.android.dynamic-feature.gradle.kts` | Android on-demand delivery modules. Declares the Compose stack in `androidMain` because nothing is inherited transitively from the base `:composeApp`. |
| `convention/src/main/kotlin/diva.android.application.gradle.kts` | Android-only application config. **Currently unused** — `:composeApp` configures itself directly. |
| `convention/src/main/kotlin/diva.android.library.gradle.kts` | Android-only (non-KMP) library config. Currently unused. |
| `convention/build.gradle.kts` | Declares the Kotlin/AGP/Compose Gradle plugin dependencies the scripts above compile against. |

## Conventions / gotchas

Three non-obvious constraints are encoded here. All three are already commented inline —
read the comment before changing the surrounding code.

- **The `jvm()` target is not shipped.** It exists purely as a host for Compose UI tests.
  `runComposeUiTest` on `androidUnitTest` dies with `Build.FINGERPRINT is null` because
  that source set is a bare JVM with no Android runtime; the desktop implementation is
  Skiko-based and needs no device.
- **`jvmShared` must go through `applyDefaultHierarchyTemplate`.** Hand-wiring it with
  `sourceSets.create` + `dependsOn` switches the default template off, which silently
  unhooks `iosMain` from the Native targets — every iOS actual stops being seen and the
  failure surfaces far from the cause.
- **`jvmTest` needs `compose.desktop.currentOs`.** Without it the Skiko native binary is
  missing and tests fail with `Cannot find libskiko-<os>-<arch>.dylib.sha256`.

Version resolution inside these scripts goes through
`extensions.getByType<VersionCatalogsExtension>().named("libs")` — precompiled script
plugins can't use the `libs.` accessor directly.

`convention/` has its own `settings.gradle.kts`; it is a separate build, so changes here
trigger a full configuration reload.

## Tests

None. Verify changes by building a representative module of each kind:

```bash
./gradlew :core:domain:compileKotlinJvm :feature:cards:jvmTest :dynamic:map_dynamic:assembleDebug
```

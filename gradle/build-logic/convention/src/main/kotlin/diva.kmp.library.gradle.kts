import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    jvmToolchain(17)
    // kotlinx-datetime 0.7 aliases Instant to kotlin.time.Instant, which is still
    // experimental in Kotlin 2.2; opt in once here rather than at every usage.
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    // Desktop JVM target. Not shipped — it exists so `commonTest` has a host that can
    // actually run Compose tests. `runComposeUiTest` on androidUnitTest dies with
    // "Build.FINGERPRINT is null" because that source set is a bare JVM with no Android
    // runtime; the desktop implementation is Skiko-based and needs no device.
    jvm()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = project.name
            isStatic = true
        }
    }

    // Android and desktop are both JVM, so actuals that are plain Java (crypto, dispatchers)
    // live in jvmSharedMain once instead of being copy-pasted into each. Only genuinely
    // platform-specific actuals — anything touching android.content.Context — stay in
    // androidMain.
    //
    // This must go through the hierarchy template rather than a manual
    // sourceSets.create + dependsOn: hand-wiring source sets switches the default template
    // off, which silently unhooks iosMain from the Native targets and every iOS actual
    // stops being seen.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withAndroidTarget()
                withJvm()
            }
        }
    }
}

android {
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val libs = catalogs.named("libs")

    compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
    defaultConfig {
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

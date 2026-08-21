import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("diva.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val compose = extensions.getByType<ComposeExtension>().dependencies

extensions.getByType<KotlinMultiplatformExtension>().apply {
    sourceSets.getByName("commonMain").dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        // Icons are no longer transitive through material3 — screens use Icons.Default/Filled.
        implementation(compose.materialIconsExtended)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)
    }
    sourceSets.getByName("androidMain").dependencies {
        implementation(compose.uiTooling)
    }
    // `runComposeUiTest` resolves to the Skiko-backed desktop implementation on the JVM
    // target, which needs the platform's native Skiko binary on the test classpath.
    // Without this it fails with "Cannot find libskiko-<os>-<arch>.dylib.sha256".
    sourceSets.getByName("jvmTest").dependencies {
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        implementation(compose.uiTest)
        implementation(compose.desktop.currentOs)
    }
}

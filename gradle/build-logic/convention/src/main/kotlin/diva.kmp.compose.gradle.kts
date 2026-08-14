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
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)
    }
    sourceSets.getByName("androidMain").dependencies {
        implementation(compose.uiTooling)
    }
}

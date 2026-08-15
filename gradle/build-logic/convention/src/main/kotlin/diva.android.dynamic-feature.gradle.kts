import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

// Every dynamic feature hosts a Compose Activity, and nothing is inherited
// transitively from the base :composeApp module, so declare the UI stack here.
val compose = extensions.getByType<ComposeExtension>().dependencies
val activityCompose = extensions.getByType<VersionCatalogsExtension>()
    .named("libs")
    .findLibrary("androidx-activity-compose").get()

extensions.getByType<KotlinMultiplatformExtension>().apply {
    sourceSets.getByName("androidMain").dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(compose.materialIconsExtended)
        implementation(activityCompose)
    }
}

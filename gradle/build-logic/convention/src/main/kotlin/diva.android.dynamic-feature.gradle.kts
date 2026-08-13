plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
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

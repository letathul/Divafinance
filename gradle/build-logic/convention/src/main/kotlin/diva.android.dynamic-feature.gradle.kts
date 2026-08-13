plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.multiplatform")
}

android {
    val catalogs = extensions.getByType<VersionCatalogsExtension>()
    val libs = catalogs.named("libs")

    compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()
    defaultConfig {
        minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

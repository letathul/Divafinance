plugins {
    alias(libs.plugins.androidDynamicFeature)
    alias(libs.plugins.kotlinMultiplatform)
}

android {
    namespace = "com.divafinance.dynamic.map"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":feature:map"))
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(project(":core:database"))
            implementation(project(":core:data"))
            implementation(project(":core:domain"))
            implementation(project(":core:ui"))
            implementation(project(":core:network"))
            implementation(project(":server"))

            implementation(project(":feature:onboarding"))
            implementation(project(":feature:dashboard"))
            implementation(project(":feature:cards"))
            implementation(project(":feature:transactions"))
            implementation(project(":feature:graphs"))
            implementation(project(":feature:feed"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:map"))
            implementation(project(":feature:scanner"))
            implementation(project(":feature:backup"))
            implementation(project(":feature:automation"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.play.feature.delivery.ktx)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.divafinance.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.divafinance.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dynamicFeatures += setOf(
        ":dynamic:map-dynamic",
        ":dynamic:scanner-dynamic",
        ":dynamic:server-dynamic"
    )
}

plugins {
    id("diva.kmp.compose")
}

android {
    namespace = "com.divafinance.feature.scanner"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            // Reads SettingsRepository for the default account, card and currency, as
            // :feature:quickadd does. No use-case wrapper exists for those reads.
            implementation(project(":core:data"))
            implementation(project(":core:model"))
            implementation(project(":core:ui"))
            implementation(project(":core:common"))
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
        }
        androidMain.dependencies {
            implementation(libs.mlkit.text.recognition)
            // Camera and photo-picker result launchers.
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

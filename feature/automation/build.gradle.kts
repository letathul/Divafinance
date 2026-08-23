plugins {
    id("diva.kmp.compose")
}

android {
    namespace = "com.divafinance.feature.automation"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            // Reads SettingsRepository to persist which automations are enabled. No
            // use-case wrapper exists for that, as in :feature:quickadd and :feature:scanner.
            implementation(project(":core:data"))
            implementation(project(":core:model"))
            implementation(project(":core:ui"))
            implementation(project(":core:common"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
        }
        androidMain.dependencies {
            // ShortcutManagerCompat — the dynamic app shortcuts a long-press shows.
            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:testing"))
        }
    }
}

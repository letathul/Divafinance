plugins {
    id("diva.kmp.compose")
}

android {
    namespace = "com.divafinance.feature.dashboard"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:ui"))
            implementation(project(":core:common"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
        }
    }
}

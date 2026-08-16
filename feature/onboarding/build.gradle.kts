plugins {
    id("diva.kmp.compose")
}

android {
    namespace = "com.divafinance.feature.onboarding"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:ui"))
            implementation(project(":core:common"))
            // The demo is offered as the final onboarding step.
            implementation(project(":feature:demo"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
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

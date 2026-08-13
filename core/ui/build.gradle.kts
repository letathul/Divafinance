plugins {
    id("diva.kmp.compose")
}

android {
    namespace = "com.divafinance.core.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(libs.kotlinx.datetime)
        }
    }
}

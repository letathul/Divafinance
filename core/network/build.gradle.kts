plugins {
    id("diva.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.divafinance.core.network"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

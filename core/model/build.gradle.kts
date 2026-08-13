plugins {
    id("diva.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.divafinance.core.model"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
    }
}

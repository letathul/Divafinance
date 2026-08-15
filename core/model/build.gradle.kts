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
            // `api` because LocalDate/Instant and @Serializable types appear in the
            // public signatures of the model classes consumed by every other module.
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
        }
    }
}

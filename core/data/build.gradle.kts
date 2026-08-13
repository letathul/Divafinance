plugins {
    id("diva.kmp.library")
}

android {
    namespace = "com.divafinance.core.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(project(":core:database"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

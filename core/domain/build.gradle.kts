plugins {
    id("diva.kmp.library")
}

android {
    namespace = "com.divafinance.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api` so feature modules see the model types returned by use cases.
            api(project(":core:model"))
            implementation(project(":core:data"))
            implementation(project(":core:common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

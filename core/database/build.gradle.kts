plugins {
    id("diva.kmp.library")
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "com.divafinance.core.database"
}

sqldelight {
    databases {
        create("DivaFinanceDb") {
            packageName.set("com.divafinance.core.database")
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

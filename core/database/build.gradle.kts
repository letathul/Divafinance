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
            // Committed schema snapshots. `1.db` captures the shape shipped before any
            // migration existed, so it must never be regenerated after a .sq edit.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // Fails the build if the .sqm files do not reproduce the .sq schema, which is the
            // only thing standing between a schema edit and a bricked upgrade.
            verifyMigrations.set(true)
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

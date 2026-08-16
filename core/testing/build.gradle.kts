plugins {
    id("diva.kmp.library")
}

android {
    namespace = "com.divafinance.core.testing"
}

/**
 * Shared test fixtures: in-memory repository fakes and sample-data builders.
 *
 * These live in `commonMain` rather than a test source set so that other modules' test
 * source sets can depend on them — Gradle test fixtures are not available for KMP source
 * sets. Nothing in production code depends on this module, so it never ships.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:data"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // Exposed so consumers inherit MainDispatcherTest without re-declaring these.
            // Exposed: installTestMainDispatcher() hands back a test dispatcher, so
            // consumers need this on their compile classpath.
            api(libs.kotlinx.coroutines.test)
        }
    }
}

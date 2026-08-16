plugins {
    id("diva.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.divafinance.server"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:network"))

            // `api` because ContentType appears in DivaServer's public constructor signature.
            api(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.sessions)
            implementation(libs.ktor.server.status.pages)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.server.auth)
            implementation(libs.ktor.serialization.kotlinx.json)

            // `api` because DivaServer.state is a StateFlow in its public surface.
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        // Runs on the JVM, where CIO can bind a real socket — the only place the
        // request/response path can be exercised end to end.
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
        }
    }
}

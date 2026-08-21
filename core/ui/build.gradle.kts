plugins {
    id("diva.kmp.compose")
}

android {
    namespace = "com.divafinance.core.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(project(":core:common"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.datetime)
        }
        // Compose UI tests run on the `jvm` host; `diva.kmp.compose` already wires the
        // Skiko-backed test runtime into jvmTest, so only the assertion library is left.
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

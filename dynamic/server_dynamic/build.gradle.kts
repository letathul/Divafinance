plugins {
    id("diva.android.dynamic-feature")
}

android {
    namespace = "com.divafinance.dynamic.server"
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":core:ui"))
    implementation(project(":server"))
    implementation(libs.koin.android)
}

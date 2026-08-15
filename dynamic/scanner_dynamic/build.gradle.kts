plugins {
    id("diva.android.dynamic-feature")
}

android {
    namespace = "com.divafinance.dynamic.scanner"
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":core:ui"))
    implementation(project(":feature:scanner"))
}

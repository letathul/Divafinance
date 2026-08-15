plugins {
    id("diva.android.dynamic-feature")
}

android {
    namespace = "com.divafinance.dynamic.demo"
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":core:ui"))
    implementation(project(":feature:demo"))
}

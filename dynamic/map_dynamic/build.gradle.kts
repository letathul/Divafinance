plugins {
    id("diva.android.dynamic-feature")
}

android {
    namespace = "com.divafinance.dynamic.map"
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":feature:map"))
}

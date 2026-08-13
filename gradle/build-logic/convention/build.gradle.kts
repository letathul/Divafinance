plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.findPlugin("kotlinMultiplatform").get().get().toString().let {
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.findVersion("kotlin").get()}"
    })
    compileOnly("com.android.tools.build:gradle:${libs.findVersion("agp").get()}")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:${libs.findVersion("compose-multiplatform").get()}")
}

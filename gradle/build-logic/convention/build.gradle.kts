import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `kotlin-dsl`
}

val libs: VersionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    val kotlinVersion = libs.findVersion("kotlin").get().requiredVersion
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")

    val agpVersion = libs.findVersion("agp").get().requiredVersion
    compileOnly("com.android.tools.build:gradle:$agpVersion")

    val composeVersion = libs.findVersion("compose-multiplatform").get().requiredVersion
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
}

pluginManagement {
    includeBuild("gradle/build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DivaFinance"

// App entry points
include(":composeApp")

// Core modules
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:network")

// Feature modules
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:cards")
include(":feature:transactions")
include(":feature:graphs")
include(":feature:feed")
include(":feature:settings")
include(":feature:map")
include(":feature:scanner")
include(":feature:backup")
include(":feature:automation")
include(":feature:demo")

// Embedded server
include(":server")

// Android dynamic feature modules
include(":dynamic:map_dynamic")
include(":dynamic:scanner_dynamic")
include(":dynamic:server_dynamic")
include(":dynamic:demo_dynamic")

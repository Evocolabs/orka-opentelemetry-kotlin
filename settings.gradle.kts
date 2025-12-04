rootProject.name = "opentelemetry-kotlin"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlinx.kover") version "0.7.6"
    }
    repositories {
        gradlePluginPortal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":api:all")

include(":api:metrics")

include(":sdk:trace")

include(":sdk:metrics")

include(":sdk:logs")

include(":sdk:testing")

include(":sdk:common")

include(":sdk:all")

include(":context")

include(":semconv")

include(":extension")
include(":exporter")

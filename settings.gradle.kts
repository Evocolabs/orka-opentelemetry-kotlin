rootProject.name = "opentelemetry-kotlin"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlinx.kover") version "0.7.6"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":api:all")

include(":api:metrics")

include(":sdk:trace")

include(":sdk:metrics")

include(":sdk:testing")

include(":sdk:common")

include(":sdk:all")

include(":context")

include(":semconv")
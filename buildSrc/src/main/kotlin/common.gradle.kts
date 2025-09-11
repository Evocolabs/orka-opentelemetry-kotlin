plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

if (System.getenv("PACKAGE_VERSION") != null) {
    project.version = System.getenv("PACKAGE_VERSION")
}

android {
    namespace = "io.opentelemetry.kotlin" // Add this line
    compileSdk = 34 // or your target SDK version

    defaultConfig {
        minSdk = 21 // or your minimum SDK version
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

repositories {
    mavenCentral()
    google {
        mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
    }
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
}
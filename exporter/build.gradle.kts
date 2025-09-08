plugins {
    id("mpplib")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.jetbrains.kotlinx.datetime)
                api(project(":api:all"))
                api(project(":sdk:common"))
                api(project(":sdk:trace"))

                implementation("org.jetbrains.kotlinx:atomicfu:0.25.0")
                implementation(libs.kotlinx.io.core)
                implementation(libs.ktor.client.core)
            }
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.test)

                implementation(libs.jetbrains.kotlinx.coroutines.core)

                implementation(libs.kotest.assertions.core)
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }

        targets.forEach {
            it.compilations.forEach { compilation ->
                when (compilation.name) {
                    "main" ->
                        compilation.apply {
                            when (this) {
                                is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation -> { // Native
                                    defaultSourceSet { dependsOn(nativeMain) }
                                }
                            }
                        }
                }
            }
        }
    }
}

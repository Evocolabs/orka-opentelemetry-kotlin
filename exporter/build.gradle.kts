plugins {
    id("mpplib")
    kotlin("plugin.serialization")
    id("com.squareup.wire") version "5.4.0"
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
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.protobuf)
                implementation(libs.wire.runtime)
                implementation(libs.wire.grpc.client)
                implementation(libs.jetbrains.kotlinx.coroutines.core)
            }
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(project(":sdk:testing"))

            implementation(libs.ktor.client.mock)

            implementation(libs.bundles.kotlin.test)

            implementation(libs.jetbrains.kotlinx.coroutines.core)

            implementation(libs.jetbrains.kotlinx.coroutines.test)

            implementation(libs.kotest.assertions.core)
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

wire {
    sourcePath {
        srcDir("src/commonMain/proto")
    }
    kotlin {
        rpcRole = "client"
    }
}

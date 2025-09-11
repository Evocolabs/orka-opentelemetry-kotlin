plugins {
    id("mpplib")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":sdk:trace"))

                implementation(libs.jetbrains.kotlinx.collections.immutable)
                implementation(libs.jetbrains.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                api(project(":sdk:testing"))

                implementation(libs.bundles.kotlin.test)

                implementation(libs.kotest.assertions.core)

                implementation(libs.jetbrains.kotlinx.coroutines.test)

                implementation(libs.benasher44.uuid)
            }
        }
    }
}
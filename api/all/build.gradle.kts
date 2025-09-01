plugins {
    id("mpplib")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":context"))

                implementation("org.jetbrains.kotlinx:atomicfu:0.25.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.test)

                implementation(libs.jetbrains.kotlinx.coroutines.core)
                
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}

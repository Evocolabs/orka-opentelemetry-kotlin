package io.opentelemetry.kotlin

actual class KotlinTarget {
    actual companion object {
        actual fun isJs(): Boolean = false

        actual fun isNative(): Boolean = true

        actual fun isJvm(): Boolean = false
    }
}
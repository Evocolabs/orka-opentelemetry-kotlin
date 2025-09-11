package io.opentelemetry.kotlin

interface Closeable {
    suspend fun close()
}

suspend inline fun <T : Closeable, R> T.useAndClose(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}

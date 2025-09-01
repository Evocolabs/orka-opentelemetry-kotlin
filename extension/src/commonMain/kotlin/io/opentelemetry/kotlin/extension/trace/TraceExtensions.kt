package io.opentelemetry.kotlin.extension.trace

import io.opentelemetry.kotlin.api.trace.Span

suspend fun<T> Span.use(block: suspend (Span) -> T): T {
    try {
        return block(this)
    } finally {
        this.end()
    }
}


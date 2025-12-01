package io.opentelemetry.kotlin.extension.trace

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.trace.Span
import io.opentelemetry.kotlin.api.trace.SpanKind
import io.opentelemetry.kotlin.api.trace.Tracer
import io.opentelemetry.kotlin.context.Context
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

suspend inline fun <T> Span.use(crossinline block: suspend (Span) -> T): T {
    try {
        val parentSpan = this
        val otelContext = coroutineContext.getOpenTelemetryContext()
        return withContext( otelContext.with(this).asContextElement()) {
            block(parentSpan)
        }
    } finally {
        this.end()
    }
}

suspend fun <T> Tracer.span(
    name: String,
    spanKind: SpanKind? = null,
    attributes: Attributes? = null,
    block: suspend (Span) -> T
): T =
    spanBuilder(name)
        .setParent(coroutineContext.getOpenTelemetryContext())
        .apply {
            spanKind?.let { setSpanKind(it) }
            attributes?.let { setAllAttributes(it) }
        }
        .startSpan()
        .use(block)

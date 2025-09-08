package io.opentelemetry.kotlin.extension.trace

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.trace.Span
import io.opentelemetry.kotlin.api.trace.SpanKind
import io.opentelemetry.kotlin.api.trace.Tracer
import io.opentelemetry.kotlin.context.Context
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

suspend fun <T> Span.use(block: suspend (Span) -> T): T {
    try {
        val parentSpan = this
        val parentContext = Context.current().with(this)
        val result = withContext(parentContext.asContextElement()) {
            return@withContext block(parentSpan)
        }
        return result
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
        .apply { spanKind?.let { setSpanKind(it) } }
        .apply { attributes?.let { setAllAttributes(it) } }
        .startSpan()
        .use(block)

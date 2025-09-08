package io.opentelemetry.kotlin.exporter

import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import io.opentelemetry.kotlin.sdk.trace.data.SpanData
import io.opentelemetry.kotlin.sdk.trace.export.SpanExporter
import kotlin.math.max
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.atomicfu.atomic

/**
 * A [io.opentelemetry.kotlin.sdk.trace.export.SpanExporter] that outputs span data to the console.
 *
 * This exporter is primarily intended for development and debugging purposes.
 * It provides a simple way to visualize trace data without requiring external dependencies.
 */
class ConsoleSpanExporter(
    private val prettyPrint: Boolean = true
) : SpanExporter {

    private val isShutdown = atomic(false)

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        if (isShutdown.value) {
            return CompletableResultCode.ofFailure()
        }

        return try {
            spans.forEach { span ->
                exportSpan(span)
            }
            CompletableResultCode.ofSuccess()
        } catch (e: Exception) {
            println("Error exporting spans: ${e.message}")
            CompletableResultCode.ofFailure()
        }
    }

    private fun exportSpan(span: SpanData) {
        if (prettyPrint) {
            exportSpanPretty(span)
        } else {
            exportSpanRaw(span)
        }
    }

    private fun exportSpanPretty(span: SpanData) {
        val startTime = formatTimestamp(span.startEpochNanos)
        val endTime = formatTimestamp(span.endEpochNanos)
        val duration = max(
            0,
            ((span.endEpochNanos - span.startEpochNanos) / 1_000_000.0).toInt()
        ) // Convert to milliseconds(span.endEpochNanos - span.startEpochNanos) / 1_000_000.0 // Convert to milliseconds

        println("┌─────────────────────────────────────────────────")
        println("│ Span: ${span.name}")
        println("│ Trace ID: ${span.traceId}")
        println("│ Span ID: ${span.spanId}")
        println("│ Parent Span ID: ${span.parentSpanId ?: "None"}")
        println("│ Kind: ${span.kind}")
        println("│ Status: ${span.status.statusCode} ${span.status.description?.let { "- $it" } ?: ""}")
        println("│ Start Time: $startTime")
        println("│ End Time: $endTime")
        println("│ Duration: $duration ms")

        if (!span.attributes.isEmpty()) {
            println("│ Attributes:")
            span.attributes.forEach { key, value ->
                println("│   $key: $value")
            }
        }

        if (span.events.isNotEmpty()) {
            println("│ Events:")
            span.events.forEach { event ->
                val eventTime = formatTimestamp(event.epochNanos)
                println("│   [$eventTime] ${event.name}")
                if (!event.attributes.isEmpty()) {
                    event.attributes.forEach { key, value ->
                        println("│     $key: $value")
                    }
                }
            }
        }

        if (span.links.isNotEmpty()) {
            println("│ Links:")
            span.links.forEach { link ->
                println("│   -> Trace: ${link.spanContext.traceId}, Span: ${link.spanContext.spanId}")
                if (!link.attributes.isEmpty()) {
                    link.attributes.forEach { key, value ->
                        println("│     $key: $value")
                    }
                }
            }
        }

        println("└─────────────────────────────────────────────────")
        println()
    }

    private fun exportSpanRaw(span: SpanData) {
        println("SPAN: name=${span.name}, traceId=${span.traceId}, spanId=${span.spanId}, " +
            "parentSpanId=${span.parentSpanId}, kind=${span.kind}, " +
            "status=${span.status.statusCode}, startTime=${span.startEpochNanos}, " +
            "endTime=${span.endEpochNanos}, attributes=${span.attributes}, " +
            "events=${span.events.size}, links=${span.links.size}")
    }

    @OptIn(ExperimentalTime::class)
    private fun formatTimestamp(epochNanos: Long): String {
        val epochMillis = epochNanos / 1_000_000
        return Instant.Companion.fromEpochMilliseconds(epochMillis).toString()
    }

    override fun flush(): CompletableResultCode {
        return if (isShutdown.value) {
            CompletableResultCode.ofFailure()
        } else {
            // In KMP, we can't explicitly flush console output, but we can return success
            // since println() calls are typically auto-flushed
            CompletableResultCode.ofSuccess()
        }
    }

    override fun shutdown(): CompletableResultCode {
        isShutdown.value = true
        return CompletableResultCode.ofSuccess()
    }

    companion object {
        /**
         * Creates a new [ConsoleSpanExporter] with default settings (pretty printing enabled).
         */
        fun create(): ConsoleSpanExporter = ConsoleSpanExporter()

        /**
         * Creates a new [ConsoleSpanExporter] with custom settings.
         */
        fun create(prettyPrint: Boolean = true): ConsoleSpanExporter =
            ConsoleSpanExporter(prettyPrint)
    }
}
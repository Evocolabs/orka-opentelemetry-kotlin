package io.opentelemetry.kotlin.exporter

import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData
import io.opentelemetry.kotlin.sdk.logs.export.LogRecordExporter
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.atomicfu.atomic

/**
 * A [io.opentelemetry.kotlin.sdk.logs.export.LogRecordExporter] that outputs log record data to the console.
 *
 * This exporter is primarily intended for development and debugging purposes.
 * It provides a simple way to visualize log data without requiring external dependencies.
 */
class ConsoleLogRecordExporter(
    private val prettyPrint: Boolean = true
) : LogRecordExporter {

    private val isShutdown = atomic(false)

    override suspend fun export(logs: Collection<LogRecordData>) {
        if (isShutdown.value) {
            throw IllegalStateException("Exporter is shutdown")
        }

        return try {
            logs.forEach { log ->
                exportLog(log)
            }
        } catch (e: Exception) {
            println("Error exporting logs: ${e.message}")
        }
    }

    private fun exportLog(log: LogRecordData) {
        if (prettyPrint) {
            exportLogPretty(log)
        } else {
            exportLogRaw(log)
        }
    }

    private fun exportLogPretty(log: LogRecordData) {
        val timestamp = formatTimestamp(log.timestampEpochNanos)
        val observedTimestamp = formatTimestamp(log.observedTimestampEpochNanos)
        val severityName = getSeverityName(log.severityNumber)

        println("┌─────────────────────────────────────────────────")
        println("│ Log Record")
        println("│ Timestamp: $timestamp")
        println("│ Observed Timestamp: $observedTimestamp")
        println("│ Severity: $severityName (${log.severityNumber})" +
            (log.severityText?.let { " - $it" } ?: ""))

        if (log.body != null) {
            println("│ Body: ${log.body}")
        }

        // Trace correlation
        if (log.spanContext.isValid()) {
            println("│ Trace ID: ${log.spanContext.traceId}")
            println("│ Span ID: ${log.spanContext.spanId}")
            println("│ Trace Flags: ${log.spanContext.traceFlags}")
        }

        // Instrumentation scope
        println("│ Instrumentation Scope: ${log.instrumentationLibraryInfo.name}" +
            (log.instrumentationLibraryInfo.version?.let { " v$it" } ?: ""))

        if (!log.attributes.isEmpty()) {
            println("│ Attributes:")
            log.attributes.forEach { key, value ->
                println("│   $key: $value")
            }
        }

        // Resource
        if (!log.resource.attributes.isEmpty()) {
            println("│ Resource:")
            log.resource.attributes.forEach { key, value ->
                println("│   $key: $value")
            }
        }

        println("└─────────────────────────────────────────────────")
        println()
    }

    private fun exportLogRaw(log: LogRecordData) {
        println("LOG: timestamp=${log.timestampEpochNanos}, " +
            "observedTimestamp=${log.observedTimestampEpochNanos}, " +
            "severity=${log.severityNumber}, severityText=${log.severityText}, " +
            "body=${log.body}, traceId=${log.spanContext.traceId}, " +
            "spanId=${log.spanContext.spanId}, " +
            "scope=${log.instrumentationLibraryInfo.name}, " +
            "attributes=${log.attributes}")
    }

    @OptIn(ExperimentalTime::class)
    private fun formatTimestamp(epochNanos: Long): String {
        val epochMillis = epochNanos / 1_000_000
        return Instant.Companion.fromEpochMilliseconds(epochMillis).toString()
    }

    private fun getSeverityName(severityNumber: Int): String {
        return when (severityNumber) {
            0 -> "UNDEFINED"
            1 -> "TRACE"
            2 -> "TRACE2"
            3 -> "TRACE3"
            4 -> "TRACE4"
            5 -> "DEBUG"
            6 -> "DEBUG2"
            7 -> "DEBUG3"
            8 -> "DEBUG4"
            9 -> "INFO"
            10 -> "INFO2"
            11 -> "INFO3"
            12 -> "INFO4"
            13 -> "WARN"
            14 -> "WARN2"
            15 -> "WARN3"
            16 -> "WARN4"
            17 -> "ERROR"
            18 -> "ERROR2"
            19 -> "ERROR3"
            20 -> "ERROR4"
            21 -> "FATAL"
            22 -> "FATAL2"
            23 -> "FATAL3"
            24 -> "FATAL4"
            else -> "UNKNOWN($severityNumber)"
        }
    }

    override suspend fun flush() {
        // no-op
    }

    override suspend fun shutdown() {
        isShutdown.value = true
    }

    companion object {
        /**
         * Creates a new [ConsoleLogRecordExporter] with default settings (pretty printing enabled).
         */
        fun create(): ConsoleLogRecordExporter = ConsoleLogRecordExporter()

        /**
         * Creates a new [ConsoleLogRecordExporter] with custom settings.
         */
        fun create(prettyPrint: Boolean = true): ConsoleLogRecordExporter =
            ConsoleLogRecordExporter(prettyPrint)
    }
}

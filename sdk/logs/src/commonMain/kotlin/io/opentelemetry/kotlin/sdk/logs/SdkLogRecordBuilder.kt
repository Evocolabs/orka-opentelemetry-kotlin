/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.common.Value
import io.opentelemetry.kotlin.api.log.LogRecordBuilder
import io.opentelemetry.kotlin.api.log.Severity
import io.opentelemetry.kotlin.api.trace.Span
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant

/**
 * SDK implementation of [LogRecordBuilder].
 * Accumulates state and creates [SdkReadWriteLogRecord] on emit().
 */
class SdkLogRecordBuilder internal constructor(
    private val instrumentationLibraryInfo: InstrumentationLibraryInfo,
    private val sharedState: LoggerSharedState,
    private val logRecordLimits: LogRecordLimits
) : LogRecordBuilder {
    private var timestampEpochNanos: Long = 0
    private var observedTimestampEpochNanos: Long = 0
    private var context: Context = Context.current()
    private var severityNumber: Int = 0
    private var severityText: String? = null
    private var body: Value<*>? = null
    private val attributes: MutableMap<AttributeKey<*>, Any> = mutableMapOf()

    override fun setTimestamp(timestamp: Long, unit: DateTimeUnit): LogRecordBuilder {
        this.timestampEpochNanos = unit.toNanoseconds(timestamp)
        return this
    }

    override fun setTimestamp(instant: Instant): LogRecordBuilder {
        this.timestampEpochNanos = instant.toEpochMilliseconds() * 1_000_000
        return this
    }

    override fun setObservedTimestamp(timestamp: Long, unit: DateTimeUnit): LogRecordBuilder {
        this.observedTimestampEpochNanos = unit.toNanoseconds(timestamp)
        return this
    }

    override fun setObservedTimestamp(instant: Instant): LogRecordBuilder {
        this.observedTimestampEpochNanos = instant.toEpochMilliseconds() * 1_000_000
        return this
    }

    override fun setContext(context: Context): LogRecordBuilder {
        this.context = context
        return this
    }

    override fun setSeverity(severity: Severity): LogRecordBuilder {
        this.severityNumber = severity.severityNumber
        this.severityText = severity.name
        return this
    }

    override fun setSeverityText(severityText: String): LogRecordBuilder {
        this.severityText = severityText
        return this
    }

    override fun setBody(body: String): LogRecordBuilder {
        this.body = io.opentelemetry.kotlin.api.common.ValueString(body)
        return this
    }

    override fun setBody(body: Value<*>): LogRecordBuilder {
        this.body = body
        return this
    }

    override fun <T : Any> setAttribute(key: AttributeKey<T>, value: T): LogRecordBuilder {
        if (key.key.isNotEmpty()) {
            attributes[key] = value
        }
        return this
    }

    override fun setAllAttributes(attributes: Attributes): LogRecordBuilder {
        if (attributes.isEmpty()) {
            return this
        }
        attributes.forEach { attributeKey, value ->
            this.attributes[attributeKey] = value
        }
        return this
    }

    override fun emit() {
        // Extract span context from the current context for trace correlation
        val spanContext = Span.fromContext(context).spanContext

        // Create the log record with proper timestamp defaulting
        val logRecord = SdkReadWriteLogRecord.create(
            spanContext,
            instrumentationLibraryInfo,
            sharedState,
            logRecordLimits,
            timestampEpochNanos,
            observedTimestampEpochNanos,
            context
        )

        // Apply all builder state to the log record
        if (severityNumber > 0) {
            logRecord.setSeverityNumber(severityNumber)
        }
        if (severityText != null) {
            logRecord.setSeverityText(severityText!!)
        }
        if (body != null) {
            logRecord.setBody(body!!)
        }

        // Apply attributes
        attributes.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            logRecord.setAttribute(key as AttributeKey<Any>, value)
        }

        // Emit the log record
        logRecord.emit()
    }
}

// Extension function for DateTimeUnit to convert to nanoseconds
private fun DateTimeUnit.toNanoseconds(value: Long): Long {
    return when (this) {
        DateTimeUnit.NANOSECOND -> value
        DateTimeUnit.MICROSECOND -> value * 1_000
        DateTimeUnit.MILLISECOND -> value * 1_000_000
        DateTimeUnit.SECOND -> value * 1_000_000_000
        DateTimeUnit.MINUTE -> value * 60_000_000_000
        DateTimeUnit.HOUR -> value * 3_600_000_000_000
        DateTimeUnit.DAY -> value * 86_400_000_000_000
        else -> throw IllegalArgumentException("Unsupported DateTimeUnit: $this")
    }
}

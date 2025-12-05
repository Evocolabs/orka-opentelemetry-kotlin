/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.common.Value
import io.opentelemetry.kotlin.api.log.Severity
import io.opentelemetry.kotlin.api.trace.Span
import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.common.Clock
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.logs.data.ImmutableLogRecordData
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData
import io.opentelemetry.kotlin.sdk.resources.Resource
import kotlinx.atomicfu.atomic
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant

/**
 * Implementation for log record that stores all mutable state with atomic fields for thread safety.
 */
internal class SdkReadWriteLogRecord
private constructor(
    override val spanContext: SpanContext,
    override val instrumentationLibraryInfo: InstrumentationLibraryInfo,
    private val logRecordLimits: LogRecordLimits,
    private val logRecordProcessor: LogRecordProcessor,
    private val clock: Clock,
    private val resource: Resource,
    private val context: Context,
    userTimestampEpochNanos: Long,
    userObservedTimestampEpochNanos: Long
) : ReadWriteLogRecord {

    // Atomic fields for thread-safe mutable state
    private val timestampEpochNanos = atomic(userTimestampEpochNanos)
    private val observedTimestampEpochNanos = atomic(userObservedTimestampEpochNanos)
    private val severityNumber = atomic(0) // UNDEFINED
    private val severityText = atomic<String?>(null)
    private val body = atomic<Value<*>?>(null)
    private val attributes = atomic<AttributesMap?>(null)
    private val hasBeenEmitted = atomic(false)

    override fun setTimestamp(timestamp: Long, unit: DateTimeUnit): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        timestampEpochNanos.value = unit.toNanoseconds(timestamp)
        return this
    }

    override fun setTimestamp(instant: Instant): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        timestampEpochNanos.value = instant.toEpochMilliseconds() * 1_000_000
        return this
    }

    override fun setObservedTimestamp(timestamp: Long, unit: DateTimeUnit): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        observedTimestampEpochNanos.value = unit.toNanoseconds(timestamp)
        return this
    }

    override fun setObservedTimestamp(instant: Instant): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        observedTimestampEpochNanos.value = instant.toEpochMilliseconds() * 1_000_000
        return this
    }

    override fun setContext(context: Context): ReadWriteLogRecord {
        // Context is immutable and set at creation time
        return this
    }

    override fun setSeverity(severity: Severity): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        severityNumber.value = severity.severityNumber
        severityText.value = severity.name
        return this
    }

    override fun setSeverityText(severityText: String): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        this.severityText.value = severityText
        return this
    }

    fun setSeverityNumber(severityNumber: Int): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        this.severityNumber.value = severityNumber
        return this
    }

    override fun setBody(body: String): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        this.body.value = io.opentelemetry.kotlin.api.common.ValueString(body)
        return this
    }

    override fun setBody(body: Value<*>): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        this.body.value = body
        return this
    }

    override fun <T> setAttribute(key: AttributeKey<T>, value: T): ReadWriteLogRecord {
        if (key.key.isEmpty()) {
            return this
        }
        if (hasBeenEmitted.value) {
            return this
        }
        // Lazy initialization of attributes map
        attributes.compareAndSet(
            null,
            AttributesMap(
                logRecordLimits.maxNumberOfAttributes,
                logRecordLimits.maxAttributeValueLength
            )
        )
        @Suppress("UNCHECKED_CAST")
        attributes.value!!.put(key as AttributeKey<Any>, value as Any)
        return this
    }

    override fun setAllAttributes(attributes: Attributes): ReadWriteLogRecord {
        if (hasBeenEmitted.value) return this
        if (attributes.isEmpty()) {
            return this
        }
        attributes.forEach { attributeKey, value ->
            @Suppress("UNCHECKED_CAST")
            setAttribute(attributeKey as AttributeKey<Any>, value)
        }
        return this
    }

    /**
     * Emits this log record by calling the processor's onEmit method.
     * This can only be called once - subsequent calls are no-ops.
     */
    override fun emit() {
        if (hasBeenEmitted.getAndSet(true)) {
            return // Already emitted
        }
        logRecordProcessor.onEmit(context, this)
    }

    override fun toLogRecordData(): LogRecordData {
        val attributesMap = attributes.value
        return ImmutableLogRecordData(
            resource = resource,
            instrumentationLibraryInfo = instrumentationLibraryInfo,
            timestampEpochNanos = timestampEpochNanos.value,
            observedTimestampEpochNanos = observedTimestampEpochNanos.value,
            spanContext = spanContext,
            severityNumber = severityNumber.value,
            severityText = severityText.value,
            body = body.value,
            attributes = attributesMap?.immutableCopy() ?: Attributes.empty(),
            totalAttributeCount = attributesMap?.totalAddedValues ?: 0
        )
    }

    companion object {
        /**
         * Creates a new SdkReadWriteLogRecord.
         *
         * @param spanContext the span context for trace correlation
         * @param instrumentationLibraryInfo the instrumentation library info
         * @param sharedState the shared state containing clock, resource, processor
         * @param logRecordLimits the limits for this log record
         * @param userTimestamp user-provided timestamp (0 if not set)
         * @param userObservedTimestamp user-provided observed timestamp (0 if not set)
         * @param context the context in which the log is emitted
         * @return a new SdkReadWriteLogRecord instance
         */
        fun create(
            spanContext: SpanContext,
            instrumentationLibraryInfo: InstrumentationLibraryInfo,
            sharedState: LoggerSharedState,
            logRecordLimits: LogRecordLimits,
            userTimestamp: Long,
            userObservedTimestamp: Long,
            context: Context
        ): SdkReadWriteLogRecord {
            val clock = sharedState.getClock()
            val resource = sharedState.getResource()
            val processor = sharedState.getActiveLogRecordProcessor()

            // Default timestamps: observedTimestamp defaults to now, timestamp defaults to observedTimestamp
            val observedTimestamp = if (userObservedTimestamp == 0L) {
                clock.now()
            } else {
                userObservedTimestamp
            }

            val timestamp = if (userTimestamp == 0L) {
                observedTimestamp
            } else {
                userTimestamp
            }

            return SdkReadWriteLogRecord(
                spanContext,
                instrumentationLibraryInfo,
                logRecordLimits,
                processor,
                clock,
                resource,
                context,
                timestamp,
                observedTimestamp
            )
        }
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

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs.data

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.common.Value
import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.resources.Resource

/**
 * Immutable representation of all data collected by a log record.
 *
 * This interface follows the OpenTelemetry Logs Data Model specification with 11 fields:
 * - Timestamp
 * - ObservedTimestamp
 * - TraceId, SpanId, TraceFlags (via SpanContext)
 * - SeverityNumber
 * - SeverityText
 * - Body (AnyValue)
 * - Attributes
 * - Resource
 * - InstrumentationScope
 */
interface LogRecordData {
    /**
     * Returns the [Resource] associated with this log record.
     *
     * @return the [Resource] associated with this log record.
     */
    val resource: Resource

    /**
     * Returns the [InstrumentationLibraryInfo] that identifies the logger that produced this
     * log record.
     *
     * @return the [InstrumentationLibraryInfo] of the logger.
     */
    val instrumentationLibraryInfo: InstrumentationLibraryInfo

    /**
     * Returns the epoch timestamp in nanoseconds when the event occurred.
     * This is the time at which the event being logged happened.
     *
     * @return the timestamp when the event occurred, in nanoseconds since epoch.
     */
    val timestampEpochNanos: Long

    /**
     * Returns the epoch timestamp in nanoseconds when the log record was observed.
     * This is the time when the log record was detected/observed by the collector.
     *
     * @return the observed timestamp in nanoseconds since epoch.
     */
    val observedTimestampEpochNanos: Long

    /**
     * Returns the [SpanContext] associated with this log record for trace correlation.
     * Contains traceId, spanId, and traceFlags per W3C Trace Context specification.
     *
     * @return the [SpanContext] for this log record.
     */
    val spanContext: SpanContext

    /**
     * Returns the severity number (0-24) following the OpenTelemetry specification:
     * - 0: UNDEFINED_SEVERITY_NUMBER
     * - 1-4: TRACE (fine-grained debugging)
     * - 5-8: DEBUG (debugging)
     * - 9-12: INFO (informational)
     * - 13-16: WARN (warning)
     * - 17-20: ERROR (error events)
     * - 21-24: FATAL (fatal errors)
     *
     * @return the severity number (0-24).
     */
    val severityNumber: Int

    /**
     * Returns the human-readable severity text (e.g., "INFO", "ERROR").
     * This is the original severity representation from the source.
     *
     * @return the severity text, or null if not set.
     */
    val severityText: String?

    /**
     * Returns the log message body as an AnyValue.
     * Can be a simple string or structured data (array, key-value map).
     *
     * @return the log body as [Value], or null if not set.
     */
    val body: Value<*>?

    /**
     * Returns the attributes associated with this log record.
     * Additional information about the specific event occurrence.
     *
     * @return the [Attributes] of this log record.
     */
    val attributes: Attributes

    /**
     * Returns the total number of attributes that were recorded on this log record.
     * This number may be larger than the number of attributes in [attributes]
     * if attributes were dropped due to limits.
     *
     * @return the total number of attributes recorded.
     */
    val totalAttributeCount: Int
}

/**
 * Internal immutable implementation of [LogRecordData].
 */
internal data class ImmutableLogRecordData(
    override val resource: Resource,
    override val instrumentationLibraryInfo: InstrumentationLibraryInfo,
    override val timestampEpochNanos: Long,
    override val observedTimestampEpochNanos: Long,
    override val spanContext: SpanContext,
    override val severityNumber: Int,
    override val severityText: String?,
    override val body: Value<*>?,
    override val attributes: Attributes,
    override val totalAttributeCount: Int
) : LogRecordData

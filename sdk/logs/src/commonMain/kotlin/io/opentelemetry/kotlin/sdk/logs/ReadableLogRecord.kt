/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData

/**
 * The extended read-only log record interface used by the SDK.
 * Provides access to the immutable data representation for export.
 */
interface ReadableLogRecord {
    /**
     * Returns the [SpanContext] associated with this log record for trace correlation.
     * Contains traceId, spanId, and traceFlags per W3C Trace Context specification.
     *
     * @return the [SpanContext] of the log record.
     */
    val spanContext: SpanContext

    /**
     * This converts this instance into an immutable [LogRecordData] instance for use in export.
     *
     * @return an immutable [LogRecordData] instance.
     */
    fun toLogRecordData(): LogRecordData

    /**
     * Returns the instrumentation library specified when creating the logger which produced
     * this log record.
     *
     * @return an instance of [InstrumentationLibraryInfo] describing the instrumentation library.
     */
    val instrumentationLibraryInfo: InstrumentationLibraryInfo
}

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.sdk.common.Clock
import io.opentelemetry.kotlin.sdk.resources.Resource

/** Builder of [SdkLoggerProvider]. */
class SdkLoggerProviderBuilder internal constructor() {
    private val logRecordProcessors: MutableList<LogRecordProcessor> = mutableListOf()
    private var clock: Clock = Clock.default
    private var resource: Resource = Resource.default
    private var logRecordLimitsSupplier: () -> LogRecordLimits = LogRecordLimits.Companion::default

    /**
     * Assign a [Clock]. [Clock] will be used each time a log record is emitted or any timestamp
     * is recorded.
     *
     * The `clock` must be thread-safe and return immediately (no remote calls, as contention free
     * as possible).
     *
     * @param clock The clock to use for all temporal needs.
     * @return this
     */
    fun setClock(clock: Clock): SdkLoggerProviderBuilder {
        this.clock = clock
        return this
    }

    /**
     * Assign a [Resource] to be attached to all log records created by Loggers.
     *
     * @param resource A Resource implementation.
     * @return this
     */
    fun setResource(resource: Resource): SdkLoggerProviderBuilder {
        this.resource = resource
        return this
    }

    /**
     * Assign an initial [LogRecordLimits] that should be used with this SDK.
     *
     * This method is equivalent to calling [setLogRecordLimits] like this
     * `setLogRecordLimits { logRecordLimits }`.
     *
     * @param logRecordLimits the limits that will be used for every log record.
     * @return this
     */
    fun setLogRecordLimits(logRecordLimits: LogRecordLimits): SdkLoggerProviderBuilder {
        logRecordLimitsSupplier = { logRecordLimits }
        return this
    }

    /**
     * Assign a supplier of [LogRecordLimits]. [LogRecordLimits] will be retrieved each time a
     * log record is emitted.
     *
     * The `logRecordLimitsSupplier` must be thread-safe and return immediately (no remote calls,
     * as contention free as possible).
     *
     * @param logRecordLimitsSupplier the supplier that will be used to retrieve the
     * [LogRecordLimits] for every log record.
     * @return this
     */
    fun setLogRecordLimits(logRecordLimitsSupplier: () -> LogRecordLimits): SdkLoggerProviderBuilder {
        this.logRecordLimitsSupplier = logRecordLimitsSupplier
        return this
    }

    /**
     * Add a LogRecordProcessor to the log record pipeline that will be built. [LogRecordProcessor]
     * will be called each time a log record is emitted.
     *
     * The `logRecordProcessor` must be thread-safe and return immediately (no remote calls, as
     * contention free as possible).
     *
     * @param logRecordProcessor the processor to be added to the processing pipeline.
     * @return this
     */
    fun addLogRecordProcessor(logRecordProcessor: LogRecordProcessor): SdkLoggerProviderBuilder {
        logRecordProcessors.add(logRecordProcessor)
        return this
    }

    /**
     * Create a new SdkLoggerProvider instance.
     *
     * @return An initialized SdkLoggerProvider.
     */
    fun build(): SdkLoggerProvider {
        return SdkLoggerProvider(
            clock,
            resource,
            logRecordLimitsSupplier,
            logRecordProcessors
        )
    }
}

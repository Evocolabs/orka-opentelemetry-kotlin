/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.Closeable
import io.opentelemetry.kotlin.api.log.Logger
import io.opentelemetry.kotlin.api.log.LoggerBuilder
import io.opentelemetry.kotlin.api.log.LoggerProvider
import io.opentelemetry.kotlin.sdk.common.Clock
import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import io.opentelemetry.kotlin.sdk.internal.ComponentRegistry
import io.opentelemetry.kotlin.sdk.resources.Resource

/**
 * `Logger` provider implementation for [LoggerProvider].
 *
 * This class is not intended to be used in application code and it is used only by the SDK.
 * However, if you need a custom implementation of the factory, you can create one as needed.
 */
class SdkLoggerProvider
internal constructor(
    clock: Clock,
    resource: Resource,
    logRecordLimitsSupplier: () -> LogRecordLimits,
    logRecordProcessors: List<LogRecordProcessor>
) : LoggerProvider, Closeable {
    internal val sharedState: LoggerSharedState
    private val loggerSdkComponentRegistry: ComponentRegistry<SdkLogger>

    init {
        sharedState =
            LoggerSharedState(
                clock,
                resource,
                logRecordLimitsSupplier,
                logRecordProcessors
            )
        loggerSdkComponentRegistry =
            ComponentRegistry { instrumentationLibraryInfo ->
                SdkLogger(sharedState, instrumentationLibraryInfo)
            }
    }

    override fun get(instrumentationScopeName: String): Logger {
        return loggerBuilder(instrumentationScopeName).build()
    }

    override fun loggerBuilder(instrumentationScopeName: String): LoggerBuilder {
        // Per the spec, both null and empty are "invalid" and a default value should be used.
        var instrumentationScopeName = instrumentationScopeName
        if (instrumentationScopeName.isEmpty()) {
            instrumentationScopeName = DEFAULT_LOGGER_NAME
        }
        return SdkLoggerBuilder(loggerSdkComponentRegistry, instrumentationScopeName)
    }

    /** Returns the [LogRecordLimits] that are currently applied to created log records. */
    val logRecordLimits: LogRecordLimits
        get() = sharedState.logRecordLimits

    /**
     * Attempts to stop all the activity for this [LoggerProvider]. Calls [LogRecordProcessor.shutdown]
     * for all registered [LogRecordProcessor]s.
     *
     * This operation may block until all the log records are processed. Must be called before
     * turning off the main application to ensure all data are processed and exported.
     *
     * After this is called all the newly created `Logger`s will be no-op.
     *
     * @return a [CompletableResultCode] which is completed when all the log records are processed.
     */
    fun shutdown(): CompletableResultCode {
        return sharedState.shutdown()
    }

    /**
     * Requests the active [LogRecordProcessor] to process all log records that have not yet been
     * processed.
     *
     * @return a [CompletableResultCode] which is completed when the flush is finished.
     */
    fun forceFlush(): CompletableResultCode {
        return sharedState.getActiveLogRecordProcessor().forceFlush()
    }

    override suspend fun close() {
        shutdown().join(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    companion object {
        private const val DEFAULT_LOGGER_NAME = ""

        /** Returns a new [SdkLoggerProviderBuilder] to construct a [SdkLoggerProvider]. */
        fun builder(): SdkLoggerProviderBuilder {
            return SdkLoggerProviderBuilder()
        }
    }
}

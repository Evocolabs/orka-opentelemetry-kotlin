/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.sdk.common.Clock
import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import io.opentelemetry.kotlin.sdk.resources.Resource
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet

/**
 * Represents the shared state/config between all Loggers created by the same LoggerProvider.
 */
internal class LoggerSharedState(
    private val clock: Clock,
    private val resource: Resource,
    private val logRecordLimitsSupplier: () -> LogRecordLimits,
    logRecordProcessors: List<LogRecordProcessor>
) {
    private val activeLogRecordProcessor = LogRecordProcessor.composite(logRecordProcessors)
    private val shutdownResult = atomic<CompletableResultCode?>(null)

    fun getClock(): Clock {
        return clock
    }

    fun getResource(): Resource {
        return resource
    }

    /** Returns the current [LogRecordLimits]. */
    val logRecordLimits: LogRecordLimits
        get() = logRecordLimitsSupplier()

    /**
     * Returns the active [LogRecordProcessor].
     *
     * @return the active [LogRecordProcessor].
     */
    fun getActiveLogRecordProcessor(): LogRecordProcessor {
        return activeLogRecordProcessor
    }

    /**
     * Returns `true` if logging has been shut down.
     *
     * @return `true` if logging has been shut down.
     */
    fun hasBeenShutdown(): Boolean {
        return shutdownResult.value != null
    }

    /**
     * Stops logging, including shutting down processors and setting [hasBeenShutdown] to `true`.
     *
     * @return a [CompletableResultCode] that will be completed when the log record processor is
     * shut down.
     */
    fun shutdown(): CompletableResultCode {
        return shutdownResult.updateAndGet { it ?: activeLogRecordProcessor.shutdown() }!!
    }
}

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.Closeable
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.common.CompletableResultCode

/**
 * LogRecordProcessor is the interface the SDK uses to allow synchronous hooks for when a
 * log record is emitted.
 */
interface LogRecordProcessor : Closeable {
    /**
     * Called when a log record is emitted.
     *
     * This method is called synchronously on the execution thread, should not throw or block the
     * execution thread.
     *
     * @param context the [Context] in which the log was emitted.
     * @param logRecord the [ReadWriteLogRecord] that was just emitted.
     */
    fun onEmit(context: Context, logRecord: ReadWriteLogRecord)

    /**
     * Returns `true` if this [LogRecordProcessor] requires emit events.
     *
     * @return `true` if this [LogRecordProcessor] requires emit events.
     */
    fun isEmitRequired(): Boolean

    /**
     * Processes all log record events that have not yet been processed and closes used resources.
     *
     * @return a [CompletableResultCode] which completes when shutdown is finished.
     */
    fun shutdown(): CompletableResultCode {
        return forceFlush()
    }

    /**
     * Processes all log record events that have not yet been processed.
     *
     * @return a [CompletableResultCode] which completes when currently queued log records are
     * finished processing.
     */
    fun forceFlush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    /**
     * Closes this [LogRecordProcessor] after processing any remaining log records, releasing any
     * resources.
     */
    override suspend fun close() {
        shutdown()
    }

    companion object {
        /**
         * Returns a [LogRecordProcessor] which simply delegates all processing to the `processors`
         * in order.
         */
        fun composite(vararg processors: LogRecordProcessor): LogRecordProcessor {
            return composite(processors.asList())
        }

        /**
         * Returns a [LogRecordProcessor] which simply delegates all processing to the `processors`
         * in order.
         */
        fun composite(processors: List<LogRecordProcessor>): LogRecordProcessor {
            if (processors.isEmpty()) {
                return NoopLogRecordProcessor.instance
            }
            return if (processors.size == 1) {
                processors.single()
            } else MultiLogRecordProcessor.create(processors)
        }
    }
}

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs.export

import io.opentelemetry.kotlin.Closeable
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData

/**
 * An interface that allows different logging services to export recorded data for log records in
 * their own format.
 *
 * To export data this MUST be registered to the `SdkLoggerProvider` using a [SimpleLogRecordProcessor]
 * or a `BatchLogRecordProcessor`.
 */
interface LogRecordExporter : Closeable {
    /**
     * Called to export log records. Note that export operations can be performed simultaneously
     * depending on the type of log record processor being used. However, the [SimpleLogRecordProcessor]
     * will ensure that only one export can occur at a time.
     *
     * @param logs the collection of log records to be exported.
     */
    suspend fun export(logs: Collection<LogRecordData>)

    /**
     * Exports the collection of log records that have not yet been exported. Note that export
     * operations can be performed simultaneously depending on the type of log record processor being
     * used. However, the SimpleLogRecordProcessor will ensure that only one export can occur at a time.
     *
     * @return the result of the flush, which is often an asynchronous operation.
     */
    suspend fun flush()

    /**
     * Called when [io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider.shutdown] is called, if this
     * `LogRecordExporter` is registered to a [io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider] object.
     */
    suspend fun shutdown()

    /** Closes this [LogRecordExporter], releasing any resources. */
    override suspend fun close() {
        shutdown()
    }

    companion object {
        /**
         * Returns a [LogRecordExporter] which simply delegates all exports to the `exporters` in order.
         *
         * Can be used to export to multiple backends using the same `LogRecordProcessor` like a
         * `SimpleLogRecordProcessor` or a `BatchLogRecordProcessor`.
         */
        fun composite(vararg exporters: LogRecordExporter): LogRecordExporter {
            return composite(exporters.toList())
        }

        /**
         * Returns a [LogRecordExporter] which simply delegates all exports to the `exporters` in order.
         *
         * Can be used to export to multiple backends using the same `LogRecordProcessor` like a
         * `SimpleLogRecordProcessor` or a `BatchLogRecordProcessor`.
         */
        fun composite(exporters: List<LogRecordExporter>): LogRecordExporter {
            if (exporters.isEmpty()) {
                return NoopLogRecordExporter.instance
            }
            return if (exporters.size == 1) {
                exporters.single()
            } else MultiLogRecordExporter.create(exporters)
        }
    }
}

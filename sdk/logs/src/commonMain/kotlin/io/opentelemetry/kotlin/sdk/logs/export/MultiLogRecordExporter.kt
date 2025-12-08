/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs.export

import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData

/**
 * Implementation of the `LogRecordExporter` that simply forwards all received log records to a list of
 * `LogRecordExporter`.
 *
 * Can be used to export to multiple backends using the same `LogRecordProcessor` like a
 * `SimpleLogRecordProcessor` or a `BatchLogRecordProcessor`.
 */
internal class MultiLogRecordExporter
private constructor(private val logRecordExporters: Array<LogRecordExporter>) : LogRecordExporter {
    override suspend fun export(logs: Collection<LogRecordData>) {
        logRecordExporters.map { logRecordExporter ->
            logRecordExporter.export(logs)
        }
    }

    /**
     * Flushes the data of all registered [LogRecordExporter]s.
     */
    override suspend fun flush() {
        logRecordExporters.map { logRecordExporter ->
            logRecordExporter.flush()
        }
    }

    override suspend fun shutdown() {
        logRecordExporters.map { logRecordExporter ->
            logRecordExporter.shutdown()
        }
    }

    companion object {
        /**
         * Constructs and returns an instance of this class.
         *
         * @param logRecordExporters the exporters log records should be sent to
         * @return the aggregate log record exporter
         */
        fun create(logRecordExporters: List<LogRecordExporter>): LogRecordExporter {
            return MultiLogRecordExporter(logRecordExporters.toTypedArray())
        }
    }
}

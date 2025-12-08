/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs.export

import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData

internal class NoopLogRecordExporter : LogRecordExporter {
    override suspend fun export(logs: Collection<LogRecordData>) {
    }

    override suspend fun flush() {
    }

    override suspend fun shutdown() {
    }

    companion object {
        private val INSTANCE: LogRecordExporter = NoopLogRecordExporter()
        val instance: LogRecordExporter
            get() = INSTANCE
    }
}

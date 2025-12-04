/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.context.Context

internal class NoopLogRecordProcessor private constructor() : LogRecordProcessor {
    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {}

    override fun isEmitRequired(): Boolean {
        return false
    }

    companion object {
        private val INSTANCE = NoopLogRecordProcessor()
        val instance: LogRecordProcessor
            get() = INSTANCE
    }
}

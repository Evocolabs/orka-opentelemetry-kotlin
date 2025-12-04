/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import kotlinx.atomicfu.atomic

/**
 * Implementation of the [LogRecordProcessor] that simply forwards all received events to a list
 * of [LogRecordProcessor]s.
 */
internal class MultiLogRecordProcessor private constructor(logRecordProcessors: List<LogRecordProcessor>) :
    LogRecordProcessor {
    private val logRecordProcessorsEmit: List<LogRecordProcessor>
    private val logRecordProcessorsAll: List<LogRecordProcessor>
    private val isShutdown = atomic(false)

    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        for (processor in logRecordProcessorsEmit) {
            processor.onEmit(context, logRecord)
        }
    }

    override fun isEmitRequired(): Boolean {
        return logRecordProcessorsEmit.isNotEmpty()
    }

    override fun shutdown(): CompletableResultCode {
        if (isShutdown.getAndSet(true)) {
            return CompletableResultCode.ofSuccess()
        }
        val results = logRecordProcessorsAll.map { processor -> processor.shutdown() }
        return CompletableResultCode.ofAll(results)
    }

    override fun forceFlush(): CompletableResultCode {
        val results = logRecordProcessorsAll.map { processor -> processor.forceFlush() }
        return CompletableResultCode.ofAll(results)
    }

    init {
        logRecordProcessorsAll = logRecordProcessors
        logRecordProcessorsEmit = logRecordProcessorsAll.filter { it.isEmitRequired() }.toList()
    }

    companion object {
        /**
         * Creates a new [MultiLogRecordProcessor].
         *
         * @param logRecordProcessorList the list of [LogRecordProcessor]s.
         * @return a new [MultiLogRecordProcessor].
         */
        fun create(logRecordProcessorList: List<LogRecordProcessor>): LogRecordProcessor {
            return MultiLogRecordProcessor(logRecordProcessorList)
        }
    }
}

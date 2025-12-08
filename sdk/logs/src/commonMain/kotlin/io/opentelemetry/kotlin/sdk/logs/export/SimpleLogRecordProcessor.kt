/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs.export

import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import io.opentelemetry.kotlin.sdk.logs.LogRecordProcessor
import io.opentelemetry.kotlin.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * An implementation of the [LogRecordProcessor] that converts the [ReadWriteLogRecord] to [LogRecordData]
 * and passes it directly to the configured exporter.
 *
 * This processor will cause all log records to be exported directly as they are emitted, meaning each
 * export request will have a single log record. Most backends will not perform well with a single log
 * per request so unless you know what you're doing, strongly consider using BatchLogRecordProcessor
 * instead, including in special environments such as serverless runtimes. [SimpleLogRecordProcessor]
 * is generally meant for logging exporters only.
 */
class SimpleLogRecordProcessor
internal constructor(private val logRecordExporter: LogRecordExporter) : LogRecordProcessor {
    private val pendingExports: MutableSet<CompletableResultCode> = mutableSetOf()
    private val isShutdown = atomic(false)

    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        if (isShutdown.value) {
            return
        }

        try {
            val logs: List<LogRecordData> = listOf(logRecord.toLogRecordData())
            val result = CompletableResultCode()
            CoroutineScope(Dispatchers.Unconfined).launch {
                try {
                    logRecordExporter.export(logs)
                    result.succeed()
                } catch (e: Exception) {
                    result.fail()
                }
            }.start()
            pendingExports.add(result)
            result.whenComplete {
                pendingExports.remove(result)
                if (!result.isSuccess) {
                    // logger.log(java.util.logging.Level.FINE, "Exporter failed")
                }
            }
        } catch (e: Exception) {
            // logger.log(java.util.logging.Level.WARNING, "Exporter threw an Exception", e)
        }
    }

    override fun isEmitRequired(): Boolean {
        return true
    }

    override fun shutdown(): CompletableResultCode {
        if (isShutdown.getAndSet(true)) {
            return CompletableResultCode.ofSuccess()
        }
        val result = CompletableResultCode()
        val flushResult: CompletableResultCode = forceFlush()
        flushResult.whenComplete {
            CoroutineScope(Dispatchers.Unconfined).launch {
                logRecordExporter.shutdown()
                if (!flushResult.isSuccess) {
                    result.fail()
                } else {
                    result.succeed()
                }
            }.start()
        }
        return result
    }

    override fun forceFlush(): CompletableResultCode {
        return CompletableResultCode.ofAll(pendingExports)
    }

    override suspend fun close() {
        shutdown().join(10, kotlinx.datetime.DateTimeUnit.SECOND)
    }

    companion object {
        /**
         * Returns a new [SimpleLogRecordProcessor] which exports log records to the [LogRecordExporter]
         * asynchronously.
         *
         * This processor will cause all log records to be exported directly as they are emitted, meaning
         * each export request will have a single log record. Most backends will not perform well with a
         * single log per request so unless you know what you're doing, strongly consider using
         * BatchLogRecordProcessor instead, including in special environments such as serverless runtimes.
         * [SimpleLogRecordProcessor] is generally meant for logging exporters only.
         */
        fun create(exporter: LogRecordExporter): LogRecordProcessor {
            return SimpleLogRecordProcessor(exporter)
        }
    }
}

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.trace.export

import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import io.opentelemetry.kotlin.sdk.trace.data.SpanData

/**
 * Implementation of the `SpanExporter` that simply forwards all received spans to a list of
 * `SpanExporter`.
 *
 * Can be used to export to multiple backends using the same `SpanProcessor` like a
 * `SimpleSampledSpansProcessor` or a `BatchSampledSpansProcessor`.
 */
internal class MultiSpanExporter
private constructor(private val spanExporters: Array<SpanExporter>) : SpanExporter {
    override suspend fun export(spans: Collection<SpanData>) {
        spanExporters.map { spanExporter ->
            spanExporter.export(spans)
        }
    }

    /**
     * Flushes the data of all registered [SpanExporter]s.
     *
     * @return the result of the operation
     */
    override suspend fun flush() {
        spanExporters.map { spanExporter ->
            spanExporter.flush()
        }
    }

    override suspend fun shutdown() {
        spanExporters.map { spanExporter ->
            spanExporter.shutdown()
        }
    }

    companion object {
        // private val logger: java.util.logging.Logger =
        //     java.util.logging.Logger.getLogger(MultiSpanExporter::class.java.getName())

        /**
         * Constructs and returns an instance of this class.
         *
         * @param spanExporters the exporters spans should be sent to
         * @return the aggregate span exporter
         */
        fun create(spanExporters: List<SpanExporter>): SpanExporter {
            return MultiSpanExporter(spanExporters.toTypedArray())
        }
    }
}

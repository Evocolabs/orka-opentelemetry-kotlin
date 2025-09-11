/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.trace.export

import io.opentelemetry.kotlin.sdk.common.CompletableResultCode
import io.opentelemetry.kotlin.sdk.trace.data.SpanData

internal class NoopSpanExporter : SpanExporter {
    override suspend fun export(spans: Collection<SpanData>) {
    }

    override suspend fun flush() {
    }

    override suspend fun shutdown() {
    }

    companion object {
        private val INSTANCE: SpanExporter = NoopSpanExporter()
        val instance: SpanExporter
            get() = INSTANCE
    }
}

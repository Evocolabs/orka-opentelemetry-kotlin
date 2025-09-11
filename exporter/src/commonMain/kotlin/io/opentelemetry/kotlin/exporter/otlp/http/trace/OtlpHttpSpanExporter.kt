package io.opentelemetry.kotlin.exporter.otlp.http.trace

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.opentelemetry.kotlin.exporter.common.encode
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSenderConfig
import io.opentelemetry.kotlin.sdk.trace.data.SpanData
import io.opentelemetry.kotlin.sdk.trace.export.SpanExporter
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse
import kotlinx.atomicfu.atomic

class OtlpHttpSpanExporter(
    private val client: HttpClient = HttpClient(),
    private val config: HttpSenderConfig
): SpanExporter {
    private val isShutdown = atomic(false)

    override suspend fun export(spans: Collection<SpanData>) {
        if (isShutdown.value) throw IllegalStateException("Exporter is shutdown")

        val encoded = spans.encode()
        val url = config.endpoint + "/v1/traces"
        val response = client.post(url) {
            val headersMap = config.headerSupplier()
            headersMap.forEach { (key, value) ->
                headers[key] = value
            }
            headers["Content-Type"] = config.contentType

            setBody(encoded.encode())
        }
        if (response.status.value in 200 until 300) {
            return
        } else {
            throw IllegalStateException("Failed to export spans, status: ${response.status}, body: ${response.bodyAsBytes().decodeToString()}")
        }
    }

    override suspend fun flush() {}


    override suspend fun shutdown() {
        isShutdown.value = true
    }

    companion object {
        fun builder(): OtlpHttpSpanExporterBuilder =
            OtlpHttpSpanExporterBuilder()
    }
}
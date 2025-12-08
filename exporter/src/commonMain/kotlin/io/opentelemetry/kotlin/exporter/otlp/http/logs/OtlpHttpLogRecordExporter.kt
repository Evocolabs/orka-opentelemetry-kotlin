package io.opentelemetry.kotlin.exporter.otlp.http.logs

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.opentelemetry.kotlin.exporter.common.encode
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSenderConfig
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData
import io.opentelemetry.kotlin.sdk.logs.export.LogRecordExporter
import kotlinx.atomicfu.atomic

/**
 * OTLP HTTP log record exporter that sends log records to an OpenTelemetry collector
 * via HTTP using protobuf encoding.
 */
class OtlpHttpLogRecordExporter(
    private val client: HttpClient = HttpClient(),
    private val config: HttpSenderConfig
): LogRecordExporter {
    private val isShutdown = atomic(false)

    override suspend fun export(logs: Collection<LogRecordData>) {
        if (isShutdown.value) throw IllegalStateException("Exporter is shutdown")

        val encoded = logs.encode()
        val url = config.endpoint + "/v1/logs"
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
            throw IllegalStateException("Failed to export log records, status: ${response.status}, body: ${response.bodyAsBytes().decodeToString()}")
        }
    }

    override suspend fun flush() {}

    override suspend fun shutdown() {
        isShutdown.value = true
    }

    companion object {
        fun builder(): OtlpHttpLogRecordExporterBuilder =
            OtlpHttpLogRecordExporterBuilder()
    }
}

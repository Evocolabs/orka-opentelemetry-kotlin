package io.opentelemetry.kotlin.exporter.sender.ktor

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.opentelemetry.kotlin.exporter.common.marshal.Marshaler
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSender
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSenderConfig
import kotlinx.io.Buffer

class KtorHttpSender(
    private val config: HttpSenderConfig
) : HttpSender {
    private val client = HttpClient()

    override suspend fun send(
        marshaler: Marshaler,
        contentLength: Int
    ): Result<HttpSender.Response> =
        runCatching {
            val response = client.post(config.endpoint) {
                val headersMap = config.headerSupplier()
                headersMap.forEach { (key, value) ->
                    headers[key] = value
                }
                headers["Content-Type"] = config.contentType

                val buffer = Buffer()
                marshaler.writeBinaryTo(buffer)
                setBody(buffer.copy())
            }

            HttpSender.Response(
                statusCode = response.status.value,
                statusMessage = response.status.description,
                responseBody = response.bodyAsBytes()
            )
        }
}
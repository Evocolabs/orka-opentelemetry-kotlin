package io.opentelemetry.kotlin.exporter.common.sender.http

import io.opentelemetry.kotlin.exporter.common.marshal.Marshaler

interface HttpSender {
    suspend fun send(
        marshaler: Marshaler,
        contentLength: Int,
    ): Result<Response>

    data class Response (
        val statusCode: Int,
        val statusMessage: String,
        val responseBody: ByteArray
    )
}
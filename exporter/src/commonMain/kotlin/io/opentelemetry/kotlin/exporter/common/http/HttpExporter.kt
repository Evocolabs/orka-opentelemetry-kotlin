package io.opentelemetry.kotlin.exporter.common.http

import io.opentelemetry.kotlin.exporter.common.marshal.Marshaler
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSender
import io.opentelemetry.kotlin.sdk.common.CompletableResultCode

class HttpExporter(
    private val httpSender: HttpSender,
) {
    suspend fun <T : Marshaler> export(
        exportRequest: T,
    ): Result<HttpSender.Response> = httpSender.send(
        exportRequest,
        exportRequest.getBinarySerializedSize()
    )
}
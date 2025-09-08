package io.opentelemetry.kotlin.exporter.common.sender.http

data class HttpSenderConfig(
    val endpoint: String,
    val contentType: String,
    val timeoutNanos: Long,
    val connectTimeoutNanos: Long,
    val headerSupplier: () -> Map<String, String>,
)
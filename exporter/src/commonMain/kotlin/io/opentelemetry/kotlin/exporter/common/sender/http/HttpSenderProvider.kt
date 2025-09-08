package io.opentelemetry.kotlin.exporter.common.sender.http

interface HttpSenderProvider {
    fun createSender(httpSenderConfig: HttpSenderConfig): HttpSender
}
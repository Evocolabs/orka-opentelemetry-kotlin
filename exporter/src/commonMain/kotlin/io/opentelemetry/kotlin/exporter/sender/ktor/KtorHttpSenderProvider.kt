package io.opentelemetry.kotlin.exporter.sender.ktor

import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSender
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSenderConfig
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSenderProvider

class KtorHttpSenderProvider: HttpSenderProvider {
    override fun createSender(httpSenderConfig: HttpSenderConfig): HttpSender =
        KtorHttpSender(httpSenderConfig)
}
package io.opentelemetry.kotlin.api.log

interface Logger {
    fun logRecordBuilder(): LogRecordBuilder
}
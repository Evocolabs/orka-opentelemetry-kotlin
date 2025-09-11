package io.opentelemetry.kotlin.exporter.otlp.http.trace

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.protobuf.protobuf
import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSenderConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OtlpHttpSpanExporterBuilder {
    companion object {
        const val DEFAULT_ENDPOINT = "http://localhost:4318"
        const val DEFAULT_CONTENT_TYPE = "application/x-protobuf"
        val DEFAULT_TIMEOUT_NANOS = 10.seconds.inWholeNanoseconds
        val DEFAULT_CONNECT_TIMEOUT_NANOS = 10.seconds.inWholeNanoseconds
    }

    var endpoint: String = DEFAULT_ENDPOINT
    var contentType: String = DEFAULT_CONTENT_TYPE
    var timeoutNanos: Long = DEFAULT_TIMEOUT_NANOS
    var connectTimeoutNanos: Long = DEFAULT_CONNECT_TIMEOUT_NANOS
    var headerSupplier: () -> Map<String, String> = { mutableMapOf() }

    var httpClient: HttpClient? = null

    fun setEndpoint(endpoint: String): OtlpHttpSpanExporterBuilder {
        this.endpoint = endpoint
        return this
    }

    fun setContentType(contentType: String): OtlpHttpSpanExporterBuilder {
        this.contentType = contentType
        return this
    }

    fun setTimeout(timeout: Duration): OtlpHttpSpanExporterBuilder {
        this.timeoutNanos = timeout.inWholeNanoseconds
        return this
    }

    fun setTimeout(timeoutNanos: Long): OtlpHttpSpanExporterBuilder {
        this.timeoutNanos = timeoutNanos
        return this
    }

    fun setConnectTimeout(connectTimeout: Duration): OtlpHttpSpanExporterBuilder {
        this.connectTimeoutNanos = connectTimeout.inWholeNanoseconds
        return this
    }

    fun setConnectTimeout(connectTimeoutNanos: Long): OtlpHttpSpanExporterBuilder {
        this.connectTimeoutNanos = connectTimeoutNanos
        return this
    }

    fun setHeaders(headers: Map<String, String>): OtlpHttpSpanExporterBuilder {
        this.headerSupplier = { headers.toMap() }
        return this
    }

    fun setHeaderSupplier(headerSupplier: () -> Map<String, String>): OtlpHttpSpanExporterBuilder {
        this.headerSupplier = headerSupplier
        return this
    }

    fun setHttpClient(httpClient: HttpClient): OtlpHttpSpanExporterBuilder {
        this.httpClient = httpClient
        return this
    }

    fun addHeader(name: String, value: String): OtlpHttpSpanExporterBuilder {
        val currentSupplier = this.headerSupplier
        this.headerSupplier = {
            currentSupplier().toMutableMap().apply {
                put(name, value)
            }
        }
        return this
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun build(): OtlpHttpSpanExporter {
        return OtlpHttpSpanExporter(
            client = httpClient ?: HttpClient {
                install(ContentNegotiation) {
                    protobuf()
                }
            },
            config = HttpSenderConfig(
                endpoint = endpoint,
                contentType = contentType,
                timeoutNanos = timeoutNanos,
                connectTimeoutNanos = connectTimeoutNanos,
                headerSupplier = headerSupplier
            )
        )
    }
}
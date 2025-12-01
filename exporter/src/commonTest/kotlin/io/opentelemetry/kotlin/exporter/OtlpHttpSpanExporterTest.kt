package io.opentelemetry.kotlin.exporter

import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.cio.Request
import io.ktor.serialization.kotlinx.protobuf.protobuf
import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.api.trace.SpanKind
import io.opentelemetry.kotlin.api.trace.TraceFlags
import io.opentelemetry.kotlin.api.trace.TraceState
import io.opentelemetry.kotlin.api.trace.TracerProvider
import io.opentelemetry.kotlin.exporter.common.encode
import io.opentelemetry.kotlin.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.common.SystemClock
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.sdk.testing.trace.TestSpanData
import io.opentelemetry.kotlin.sdk.trace.SdkTracerProvider
import io.opentelemetry.kotlin.sdk.trace.SdkTracerProviderBuilder
import io.opentelemetry.kotlin.sdk.trace.SpanProcessor
import io.opentelemetry.kotlin.sdk.trace.data.SpanData
import io.opentelemetry.kotlin.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import okio.ByteString.Companion.decodeHex
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.test.currentTime

class OtlpHttpSpanExporterTest {

    @OptIn(ExperimentalSerializationApi::class)
    fun setUpMockClient(onRequest: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine {
            onRequest(it)
        }) {
            install(ContentNegotiation) {
                protobuf()
            }
        }

    @Test
    fun `successfully export single span`() {
        runTest {
            val sentRequests: MutableList<HttpRequestData> = mutableListOf()

            val mockClient = setUpMockClient {
                sentRequests.add(it)
                respondOk()
            }

            val exporter = OtlpHttpSpanExporter
                .builder()
                .setEndpoint("https://otelcollector.hiorka.com")
//                .setHttpClient(mockClient)
                .build()
            val name = "TestSpan"
            val spanKind = SpanKind.INTERNAL
            val traceId = "1234567890abcdef1234567890abcdef"
            val spanId = "1234567890abcdef"
            val traceFlags = TraceFlags.default
            val traceState = TraceState.default

            val spanData: SpanData =
                TestSpanData
                    .builder()
                    .setName(name)
                    .setKind(spanKind)
                    .setTotalAttributeCount(1)
                    .setInstrumentationLibraryInfo(InstrumentationLibraryInfo.create("io.opentelemetry.kotlin", "0.0.1"))
                    .setStartEpochNanos(SystemClock.instance.now())
                    .setEndEpochNanos(SystemClock.instance.now() + 1.seconds.inWholeNanoseconds)
                    .setSpanContext(
                        SpanContext.create(
                            traceIdHex = traceId,
                            spanIdHex = spanId,
                            traceFlags = traceFlags,
                            traceState = traceState
                        )
                    )
                    .setAttributes(Attributes.of(AttributeKey.stringKey("device.name"), "奥卡一号"))
                    .setResource(Resource.default)
                    .build()

            exporter.export(mutableListOf(spanData))
            sentRequests.size shouldBe 1
            val request = sentRequests[0]
            request.body.contentType shouldBe ContentType("application", "x-protobuf")

            val traceRequest = ExportTraceServiceRequest.ADAPTER.decode(request.body.toByteArray())
            traceRequest.resource_spans.size shouldBe 1
            val resourceSpan = traceRequest.resource_spans[0]
            resourceSpan.scope_spans.size shouldBe 1
            val scopeSpan = resourceSpan.scope_spans[0]
            scopeSpan.spans.size shouldBe 1
            val span = scopeSpan.spans[0]
            span.name shouldBe name
            span.kind shouldBe spanKind.encode()
            span.trace_id shouldBe traceId.decodeHex()
            span.span_id shouldBe spanId.decodeHex()
        }
    }

//    @Test
//    fun `successfully doing a span`() {
//        runTest {
//            val tracerProvider = SdkTracerProvider
//                .builder()
//                .addSpanProcessor(
//                    SpanProcessor.composite(
//                        listOf(
//                            SimpleSpanProcessor.create(
//                                OtlpHttpSpanExporter
//                                    .builder()
//                                    .build()
//                            )
//                        )
//                    )
//                )
//                .build()
//            val tracer = tracerProvider.get("test")
//            val span = tracer.spanBuilder("test").startSpan()
//            span.setAttribute("TestKey", "TestValue")
//            span.addEvent("TestEvent")
//            span.end()
//        }
//    }
}

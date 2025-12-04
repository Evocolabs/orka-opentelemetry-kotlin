package io.opentelemetry.kotlin.sdk.extension

import io.kotest.matchers.shouldBe
import io.opentelemetry.kotlin.extension.trace.getOpenTelemetryContext
import io.opentelemetry.kotlin.extension.trace.span
import io.opentelemetry.kotlin.sdk.testing.time.TestClock
import io.opentelemetry.kotlin.sdk.trace.SdkTracerProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDerivingSpan() {
        val clock = TestClock.create()
        val tracerProvider =
            SdkTracerProvider
                .builder()
                .setClock(clock)
//                .addSpanProcessor(
//                    SimpleSpanProcessor
//                        .create(ConsoleSpanExporter())
//                )
                .build()

        val tracer = tracerProvider.get("Deriving Span Test")
        runTest {
            tracer.span("TestDrivingSpan") { parentSpan ->
                val parentTraceId = parentSpan.spanContext.traceId
                tracer.span("ChildSpan") {
                    println("Hello World in Child Span")

                    // 在 clock.advance 之前检查上下文
                    val contextBefore = coroutineContext.getOpenTelemetryContext()
                    println("Context inside child block: $contextBefore")
                    println("Context inside child block - coroutineContext keys: ${coroutineContext.fold(emptyList<String>()) { acc, element -> acc + element.key.toString() }}")

                    it.spanContext.traceId shouldBe parentTraceId
                }
                val contextAfter = coroutineContext.getOpenTelemetryContext()
                println("Context inside parent block after child block: $contextAfter")
                println("Context inside parent block after child block - coroutineContext keys: ${coroutineContext.fold(emptyList<String>()) { acc, element -> acc + element.key.toString() }}")
//                Span.fromContext(coroutineContext.getOpenTelemetryContext()).spanContext.traceId shouldBe parentTraceId
//                Span.fromContext(coroutineContext.getOpenTelemetryContext()).spanContext.spanId shouldBe parentSpan.spanContext.spanId
            }
        }
    }
}


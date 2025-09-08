package io.opentelemetry.kotlin.sdk.extension

import io.kotest.matchers.shouldBe
import io.opentelemetry.kotlin.extension.trace.span
import io.opentelemetry.kotlin.sdk.testing.time.TestClock
import io.opentelemetry.kotlin.sdk.trace.SdkTracerProvider
import io.opentelemetry.kotlin.sdk.trace.export.ConsoleSpanExporter
import io.opentelemetry.kotlin.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class DerivingSpanTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDerivingSpan() {
        val clock = TestClock.create()
        val tracerProvider =
            SdkTracerProvider
                .builder()
                .setClock(clock)
                .addSpanProcessor(
                    SimpleSpanProcessor
                        .create(ConsoleSpanExporter())
                )
                .build()

        val tracer = tracerProvider.get("Deriving Span Test")
        runTest {
            tracer.span("TestDrivingSpan") { parentSpan ->
                val parentTraceId = parentSpan.spanContext.traceId
                tracer.span("ChildSpan") {
                    println("Hello World in Child Span")
                    clock.advance(1.seconds)
                    it.spanContext.traceId shouldBe parentTraceId
                }
                clock.advance(1.seconds)
            }
        }
    }
}


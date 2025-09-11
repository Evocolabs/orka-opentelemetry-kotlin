package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.api.trace.TraceFlags
import io.opentelemetry.kotlin.api.trace.TraceState

class TestSpanContext(
    override val traceId: String = "",
    override val spanId: String = "",
    override val traceFlags: TraceFlags = TraceFlags.default,
    override val traceState: TraceState = TraceState.default,
    override val isRemote: Boolean = false
) : SpanContext
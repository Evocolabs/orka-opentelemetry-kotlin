package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.api.trace.TraceState
import io.opentelemetry.kotlin.api.trace.TraceStateBuilder

class TestTraceState(
    private val states: Map<String, String>
) : TraceState {
    override fun get(key: String): String = states[key] ?: ""

    override fun size(): Int = states.size

    override val isEmpty: Boolean = states.isEmpty()
    override fun forEach(consumer: (String, String) -> Unit) = states.forEach {
        consumer(it.key, it.value)
    }

    override fun asMap(): Map<String, String> = states

    override fun toBuilder(): TraceStateBuilder {
        TODO("Not yet implemented")
    }
}
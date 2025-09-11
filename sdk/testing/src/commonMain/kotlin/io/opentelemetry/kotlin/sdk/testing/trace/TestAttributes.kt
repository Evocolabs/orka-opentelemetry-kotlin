package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.common.AttributesBuilder

class TestAttributes(
    private val attrs: Map<AttributeKey<*>, Any>
) : Attributes {
    override val size: Int = attrs.size

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: AttributeKey<T>): T? = attrs[key] as? T

    override fun forEach(consumer: (AttributeKey<*>, Any) -> Unit) {
        attrs.forEach {
            consumer(it.key, it.value)
        }
    }

    override fun isEmpty(): Boolean = attrs.isEmpty()

    override fun asMap(): Map<AttributeKey<*>, Any> = attrs

    override fun toBuilder(): AttributesBuilder {
        TODO("Not yet implemented")
    }
}
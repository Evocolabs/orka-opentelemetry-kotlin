package io.opentelemetry.kotlin.api.common

data class ValueArray(override val value: List<Value<*>>): Value<List<Value<*>>> {
    override val type: ValueType = ValueType.ARRAY
    override fun asString(): String = value.joinToString(prefix = "[", postfix = "]", separator = ",") { it.asString() }
}
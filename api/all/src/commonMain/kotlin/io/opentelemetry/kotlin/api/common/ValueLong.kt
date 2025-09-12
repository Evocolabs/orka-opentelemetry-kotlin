package io.opentelemetry.kotlin.api.common

data class ValueLong(override val value: Long): Value<Long> {
    override val type: ValueType = ValueType.LONG
    override fun asString(): String = value.toString()
}
package io.opentelemetry.kotlin.api.common

data class ValueBoolean(override val value: Boolean): Value<Boolean> {
    override val type: ValueType = ValueType.BOOLEAN
    override fun asString(): String = value.toString()
}
package io.opentelemetry.kotlin.api.common

data class ValueDouble(override val value: Double): Value<Double> {
    override val type: ValueType = ValueType.DOUBLE
    override fun asString(): String = value.toString()
}
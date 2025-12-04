package io.opentelemetry.kotlin.api.common

data class ValueString(override val value: String): Value<String> {
    override val type: ValueType = ValueType.STRING
    override fun asString(): String = value
}
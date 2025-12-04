package io.opentelemetry.kotlin.api.common

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ValueBytes(override val value: ByteArray): Value<ByteArray> {
    override val type: ValueType = ValueType.BYTES
    @OptIn(ExperimentalEncodingApi::class)
    override fun asString(): String = Base64.encode(value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ValueBytes

        if (!value.contentEquals(other.value)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
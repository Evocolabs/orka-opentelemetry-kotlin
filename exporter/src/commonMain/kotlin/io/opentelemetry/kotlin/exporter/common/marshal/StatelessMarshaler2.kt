package io.opentelemetry.kotlin.exporter.common.marshal

interface StatelessMarshaler2<K, V> {
    fun getBinarySerializedSize(key: K, value: V, context: MarshalerContext): Int
    fun writeTo(output: Serializer, key: K, value: V, context: MarshalerContext)
}
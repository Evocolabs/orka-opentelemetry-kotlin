package io.opentelemetry.kotlin.exporter.common.marshal

interface StatelessMarshaler<T> {
    fun getBinarySerializedSize(value: T, context: MarshalerContext): Int

    fun writeTo(output: Serializer, value: T, context: MarshalerContext)
}
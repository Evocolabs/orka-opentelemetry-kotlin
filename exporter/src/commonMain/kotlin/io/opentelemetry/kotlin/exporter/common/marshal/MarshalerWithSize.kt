package io.opentelemetry.kotlin.exporter.common.marshal

abstract class MarshalerWithSize(
    private val size: Int
): Marshaler() {
    override fun getBinarySerializedSize(): Int = size
}
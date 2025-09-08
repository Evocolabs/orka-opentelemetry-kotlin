package io.opentelemetry.kotlin.exporter.common.marshal

import kotlinx.io.Sink

abstract class Marshaler {
    fun writeBinaryTo(output: Sink) {
        ProtoSerializer(output).use { serializer ->
            writeTo(serializer)
        }
    }

    /** Returns the number of bytes this Marshaler will write in proto binary format.  */
    abstract fun getBinarySerializedSize(): Int

    abstract fun writeTo(output: Serializer)
}
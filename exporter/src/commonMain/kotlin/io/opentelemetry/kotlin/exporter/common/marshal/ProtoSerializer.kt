/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.exporter.common.marshal

import io.opentelemetry.kotlin.api.internal.OtelEncodingUtils
import io.opentelemetry.kotlin.api.trace.SpanId
import io.opentelemetry.kotlin.api.trace.TraceId
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlin.native.concurrent.ThreadLocal

/** Serializer for the protobuf binary wire format.  */
class ProtoSerializer(output: Sink) : Serializer() {
    private val output: CodedOutputStream = CodedOutputStream.newInstance(output)
    private val idCache: MutableMap<String, ByteArray> = ID_CACHE

    override fun writeTraceId(field: ProtoFieldInfo, traceId: String) {
        val traceIdBytes: ByteArray =
            idCache.getOrPut(
                traceId
            ) {
                OtelEncodingUtils.bytesFromBase16(traceId, TraceId.length)
            }
        writeBytes(field, traceIdBytes)
    }

    override fun writeTraceId(field: ProtoFieldInfo, traceId: String, context: MarshalerContext) {
        var traceIdBytes = idCache[traceId]
        if (traceIdBytes == null) {
            traceIdBytes = context.traceIdBuffer
            OtelEncodingUtils.bytesFromBase16(traceId, TraceId.length, traceIdBytes)
            idCache.put(traceId, traceIdBytes)
        }
        writeBytes(field, traceIdBytes)
    }


    override fun writeSpanId(field: ProtoFieldInfo, spanId: String) {
        val spanIdBytes: ByteArray =
            idCache.getOrPut(
                spanId) { OtelEncodingUtils.bytesFromBase16(spanId, SpanId.length) }
        writeBytes(field, spanIdBytes)
    }


    override fun writeSpanId(field: ProtoFieldInfo, spanId: String, context: MarshalerContext) {
        var spanIdBytes = idCache[spanId]
        if (spanIdBytes == null) {
            spanIdBytes = context.spanIdBuffer
            OtelEncodingUtils.bytesFromBase16(spanId, SpanId.length, spanIdBytes)
            idCache.put(spanId, spanIdBytes)
        }
        writeBytes(field, spanIdBytes)
    }


    override fun writeBool(field: ProtoFieldInfo, value: Boolean) {
        output.writeUInt32NoTag(field.tag)
        output.writeBoolNoTag(value)
    }


    override fun writeEnum(field: ProtoFieldInfo, enumValue: ProtoEnumInfo) {
        output.writeUInt32NoTag(field.tag)
        output.writeEnumNoTag(enumValue.enumNumber)
    }


    override fun writeUint32(field: ProtoFieldInfo, value: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeUInt32NoTag(value)
    }


    override fun writeSInt32(field: ProtoFieldInfo, value: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeSInt32NoTag(value)
    }


    override fun writeint32(field: ProtoFieldInfo, value: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeInt32NoTag(value)
    }


    override fun writeInt64(field: ProtoFieldInfo, value: Long) {
        output.writeUInt32NoTag(field.tag)
        output.writeInt64NoTag(value)
    }


    override fun writeUInt64(field: ProtoFieldInfo, value: Long) {
        output.writeUInt32NoTag(field.tag)
        output.writeUInt64NoTag(value)
    }


    override fun writeFixed64(field: ProtoFieldInfo, value: Long) {
        output.writeUInt32NoTag(field.tag)
        output.writeFixed64NoTag(value)
    }


    override fun writeFixed64Value(value: Long) {
        output.writeFixed64NoTag(value)
    }


    override fun writeUInt64Value(value: Long) {
        output.writeUInt64NoTag(value)
    }


    override fun writeFixed32(field: ProtoFieldInfo, value: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeFixed32NoTag(value)
    }


    override fun writeDouble(field: ProtoFieldInfo, value: Double) {
        output.writeUInt32NoTag(field.tag)
        output.writeDoubleNoTag(value)
    }


    override fun writeDoubleValue(value: Double) {
        output.writeDoubleNoTag(value)
    }


    override fun writeString(field: ProtoFieldInfo, utf8Bytes: ByteArray) {
        writeBytes(field, utf8Bytes)
    }


    override fun writeString(
        field: ProtoFieldInfo, string: String, utf8Length: Int, context: MarshalerContext) {
        output.writeUInt32NoTag(field.tag)
        output.writeUInt32NoTag(utf8Length)

        StatelessMarshalerUtil.writeUtf8(output, string, utf8Length, context)
    }


    override fun writeRepeatedString(field: ProtoFieldInfo, utf8Bytes: Array<ByteArray>) {
        for (value in utf8Bytes) {
            writeString(field, value)
        }
    }


    override fun writeBytes(field: ProtoFieldInfo, value: ByteArray) {
        output.writeUInt32NoTag(field.tag)
        output.writeByteArrayNoTag(value)
    }


    override fun writeByteBuffer(field: ProtoFieldInfo, value: Buffer) {
        output.writeUInt32NoTag(field.tag)
        output.writeByteBufferNoTag(value)
    }


    override fun writeStartMessage(field: ProtoFieldInfo, protoMessageSize: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeUInt32NoTag(protoMessageSize)
    }

    override fun writeEndMessage() {
        // Do nothing
    }


    override fun writeStartRepeatedPrimitive(
        field: ProtoFieldInfo, protoSizePerElement: Int, numElements: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeUInt32NoTag(protoSizePerElement * numElements)
    }

    override fun writeEndRepeatedPrimitive() {
        // Do nothing
    }


    override fun writeStartRepeatedVarint(field: ProtoFieldInfo, payloadSize: Int) {
        output.writeUInt32NoTag(field.tag)
        output.writeUInt32NoTag(payloadSize)
    }

    override fun writeEndRepeatedVarint() {
        // Do nothing
    }


    override fun serializeRepeatedMessage(field: ProtoFieldInfo, repeatedMessage: Array<Marshaler>) {
        for (message in repeatedMessage) {
            serializeMessage(field, message)
        }
    }


    override fun serializeRepeatedMessage(
        field: ProtoFieldInfo, repeatedMessage: MutableList<out Marshaler>) {
        for (message in repeatedMessage) {
            serializeMessage(field, message)
        }
    }


    override fun <T> serializeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: MutableList<out T>,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext) {
        for (i in messages.indices) {
            val message: T = messages[i]
            writeStartMessage(field, context.size)
            marshaler.writeTo(this, message, context)
            writeEndMessage()
        }
    }

    override fun writeStartRepeated(field: ProtoFieldInfo) {
        // Do nothing
    }

    override fun writeEndRepeated() {
        // Do nothing
    }


    override fun writeStartRepeatedElement(field: ProtoFieldInfo, protoMessageSize: Int) {
        writeStartMessage(field, protoMessageSize)
    }

    override fun writeEndRepeatedElement() {
        writeEndMessage()
    }


    override fun writeSerializedMessage(protoSerialized: ByteArray, jsonSerialized: String) {
        output.writeRawBytes(protoSerialized)
    }


    override fun close() {
        output.flush()
        idCache.clear()
    }

    companion object {
        // Cache ID conversion to bytes since we know it's common to useAndClose the same ID multiple times within
        // a single export (trace ID and parent span ID).
        // In practice, there is often only one thread that calls this code in the BatchSpanProcessor so
        // reusing buffers for the thread is almost free. Even with multiple threads, it should still be
        // worth it and is common practice in serialization libraries such as Jackson.
        private val ID_CACHE: MutableMap<String, ByteArray> = mutableMapOf()
    }
}
package io.opentelemetry.kotlin.exporter.common.marshal

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Attributes
import kotlinx.io.Buffer

abstract class Serializer internal constructor() : AutoCloseable {
    /** Serializes a trace ID field.  */
    fun serializeTraceId(field: ProtoFieldInfo, traceId: String?) {
        if (traceId == null) {
            return
        }
        writeTraceId(field, traceId)
    }

    fun serializeTraceId(
        field: ProtoFieldInfo, traceId: String?, context: MarshalerContext) {
        if (traceId == null) {
            return
        }
        writeTraceId(field, traceId, context)
    }

    protected abstract fun writeTraceId(field: ProtoFieldInfo, traceId: String)

    protected open fun writeTraceId(field: ProtoFieldInfo, traceId: String, context: MarshalerContext) {
        writeTraceId(field, traceId)
    }

    /** Serializes a span ID field.  */
    fun serializeSpanId(field: ProtoFieldInfo, spanId: String?) {
        if (spanId == null) {
            return
        }
        writeSpanId(field, spanId)
    }

    fun serializeSpanId(
        field: ProtoFieldInfo, spanId: String?, context: MarshalerContext) {
        if (spanId == null) {
            return
        }
        writeSpanId(field, spanId, context)
    }


    protected abstract fun writeSpanId(field: ProtoFieldInfo, spanId: String)


    protected open fun writeSpanId(field: ProtoFieldInfo, spanId: String, context: MarshalerContext) {
        writeSpanId(field, spanId)
    }

    /** Serializes a protobuf `bool` field.  */
    fun serializeBool(field: ProtoFieldInfo, value: Boolean) {
        if (!value) {
            return
        }
        writeBool(field, value)
    }

    /** Writes a protobuf `bool` field, even if it matches the default value.  */
    abstract fun writeBool(field: ProtoFieldInfo, value: Boolean)

    /** Serializes a protobuf `enum` field.  */

    fun serializeEnum(field: ProtoFieldInfo, enumValue: ProtoEnumInfo) {
        if (enumValue.enumNumber == 0) {
            return
        }
        writeEnum(field, enumValue)
    }


    protected abstract fun writeEnum(field: ProtoFieldInfo, enumValue: ProtoEnumInfo)

    /** Serializes a protobuf `uint32` field.  */

    fun serializeUInt32(field: ProtoFieldInfo, value: Int) {
        if (value == 0) {
            return
        }
        writeUint32(field, value)
    }


    protected abstract fun writeUint32(field: ProtoFieldInfo, value: Int)

    /** Serializes a protobuf `sint32` field.  */

    fun serializeSInt32(field: ProtoFieldInfo, value: Int) {
        if (value == 0) {
            return
        }
        writeSInt32(field, value)
    }


    protected abstract fun writeSInt32(info: ProtoFieldInfo, value: Int)

    /** Serializes a protobuf `int32` field.  */

    fun serializeInt32(field: ProtoFieldInfo, value: Int) {
        if (value == 0) {
            return
        }
        writeint32(field, value)
    }

    /** Serializes a protobuf `int32` field.  */

    fun serializeInt32Optional(field: ProtoFieldInfo, value: Int) {
        writeint32(field, value)
    }

    /** Serializes a protobuf `int32` field.  */

    fun serializeInt32Optional(field: ProtoFieldInfo, value: Int?) {
        if (value != null) {
            serializeInt32Optional(field, value)
        }
    }


    protected abstract fun writeint32(field: ProtoFieldInfo, value: Int)

    /** Serializes a protobuf `int64` field.  */

    fun serializeInt64(field: ProtoFieldInfo, value: Long) {
        if (value == 0L) {
            return
        }
        writeInt64(field, value)
    }

    /** Serializes a protobuf `uint64` field.  */

    fun serializeUInt64(field: ProtoFieldInfo, value: Long) {
        if (value == 0L) {
            return
        }
        writeUInt64(field, value)
    }

    /** Writes a protobuf `int64` field, even if it matches the default value.  */

    abstract fun writeInt64(field: ProtoFieldInfo, value: Long)

    /** Writes a protobuf `uint64` field, even if it matches the default value.  */

    abstract fun writeUInt64(field: ProtoFieldInfo, value: Long)

    /** Serializes a protobuf `fixed64` field.  */

    fun serializeFixed64(field: ProtoFieldInfo, value: Long) {
        if (value == 0L) {
            return
        }
        writeFixed64(field, value)
    }

    /** Serializes a protobuf `fixed64` field.  */

    fun serializeFixed64Optional(field: ProtoFieldInfo, value: Long) {
        writeFixed64(field, value)
    }


    protected abstract fun writeFixed64(field: ProtoFieldInfo, value: Long)


    protected abstract fun writeFixed64Value(value: Long)


    protected abstract fun writeUInt64Value(value: Long)

    /**
     * Serializes a byte as a protobuf `fixed32` field. Ensures that there is no sign
     * propagation if the high bit in the byte is set.
     */

    fun serializeByteAsFixed32(field: ProtoFieldInfo, value: Byte) {
        serializeFixed32(field, (value.toInt()) and 0xff)
    }

    /** Serializes a protobuf `fixed32` field.  */

    fun serializeFixed32(field: ProtoFieldInfo, value: Int) {
        if (value == 0) {
            return
        }
        writeFixed32(field, value)
    }


    protected abstract fun writeFixed32(field: ProtoFieldInfo, value: Int)

    /** Serializes a proto buf `double` field.  */

    fun serializeDouble(field: ProtoFieldInfo, value: Double) {
        if (value == 0.0) {
            return
        }
        writeDouble(field, value)
    }

    /** Serializes a proto buf `double` field.  */

    fun serializeDoubleOptional(field: ProtoFieldInfo, value: Double) {
        writeDouble(field, value)
    }

    /** Writes a protobuf `double` field, even if it matches the default value.  */

    abstract fun writeDouble(field: ProtoFieldInfo, value: Double)


    protected abstract fun writeDoubleValue(value: Double)

    /**
     * Serializes a protobuf `string` field. `utf8Bytes` is the UTF8 encoded bytes of the
     * string to serialize.
     */

    fun serializeString(field: ProtoFieldInfo, utf8Bytes: ByteArray) {
        if (utf8Bytes.isEmpty()) {
            return
        }
        writeString(field, utf8Bytes)
    }

    /**
     * Serializes a protobuf `repeated string` field. `utf8Bytes` is the UTF8 encoded
     * bytes of the strings to serialize.
     */

    fun serializeRepeatedString(field: ProtoFieldInfo, utf8Bytes: Array<ByteArray>) {
        if (utf8Bytes.isEmpty()) {
            return
        }
        writeRepeatedString(field, utf8Bytes)
    }

    /**
     * Serializes a protobuf `string` field. `string` is the value to be serialized and
     * `utf8Length` is the length of the string after it is encoded in UTF8. This method reads
     * elements from context, use together with [ ][StatelessMarshalerUtil.sizeStringWithContext].
     */

    fun serializeStringWithContext(
        field: ProtoFieldInfo, string: String?, context: MarshalerContext) {
        if (string == null || string.isEmpty()) {
            return
        }
        if (context.marshalStringNoAllocation()) {
            writeString(field, string, context.size, context)
        } else {
            val valueUtf8: ByteArray = context.getData(ByteArray::class)
            writeString(field, valueUtf8)
        }
    }

    /** Writes a protobuf `string` field, even if it matches the default value.  */

    abstract fun writeString(field: ProtoFieldInfo, utf8Bytes: ByteArray)


    abstract fun writeString(
        field: ProtoFieldInfo, string: String, utf8Length: Int, context: MarshalerContext)

    /** Writes a protobuf `repeated string` field, even if it matches the default value.  */

    abstract fun writeRepeatedString(field: ProtoFieldInfo, utf8Bytes: Array<ByteArray>)

    /**
     * Serializes a protobuf `bytes` field. Writes all content of the ByteBuffer regardless of
     * the current position and limit. Does not alter the position or limit of the provided
     * ByteBuffer.
     */

    fun serializeByteBuffer(field: ProtoFieldInfo, value: Buffer) {
        writeByteBuffer(field, value)
    }

    abstract fun writeBytes(field: ProtoFieldInfo, value: ByteArray)


    abstract fun writeByteBuffer(field: ProtoFieldInfo, value: Buffer)


    protected abstract fun writeStartMessage(field: ProtoFieldInfo, protoMessageSize: Int)


    protected abstract fun writeEndMessage()

    /** Serializes a protobuf embedded {@code message}. */ 
    fun serializeMessage(field: ProtoFieldInfo, message: Marshaler) {
        writeStartMessage(field, message.getBinarySerializedSize())
        message.writeTo(this)
        writeEndMessage()
    }

    /**
     * Serializes a protobuf embedded `message`. This method adds elements to context, use
     * together with [StatelessMarshalerUtil.sizeMessageWithContext].
     */
    fun <T> serializeMessageWithContext(
        field: ProtoFieldInfo,
        message: T,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext
    ) {
        writeStartMessage(field, context.size)
        marshaler.writeTo(this, message, context)
        writeEndMessage()
    }

    /**
     * Serializes a protobuf embedded `message`. This method adds elements to context, use
     * together with [StatelessMarshalerUtil.sizeMessageWithContext].
     */
    fun <K, V> serializeMessageWithContext(
        field: ProtoFieldInfo,
        key: K,
        value: V,
        marshaler: StatelessMarshaler2<K, V>,
        context: MarshalerContext
    ) {
        writeStartMessage(field, context.size)
        marshaler.writeTo(this, key, value, context)
        writeEndMessage()
    }

    protected abstract fun writeStartRepeatedPrimitive(
        field: ProtoFieldInfo, protoSizePerElement: Int, numElements: Int)


    protected abstract fun writeEndRepeatedPrimitive()


    protected abstract fun writeStartRepeatedVarint(field: ProtoFieldInfo, payloadSize: Int)

    protected abstract fun writeEndRepeatedVarint()

    /** Serializes a `repeated int32` field.  */
    
    fun serializeRepeatedInt32(field: ProtoFieldInfo, values: MutableList<Int>) {
        if (values.isEmpty()) {
            return
        }

        var payloadSize = 0
        for (v in values) {
            payloadSize += CodedOutputStream.computeInt32SizeNoTag(v)
        }

        writeStartRepeatedVarint(field, payloadSize)
        for (value in values) {
            writeUInt64Value(value.toLong())
        }
        writeEndRepeatedVarint()
    }

    /** Serializes a `repeated fixed64` field.  */
    
    fun serializeRepeatedFixed64(field: ProtoFieldInfo, values: MutableList<Long>) {
        if (values.isEmpty()) {
            return
        }
        writeStartRepeatedPrimitive(field, WireFormat.FIXED64_SIZE, values.size)
        for (i in values.indices) {
            val value = values[i]
            writeFixed64Value(value)
        }
        writeEndRepeatedPrimitive()
    }

    /** Serializes a `repeated fixed64` field.  */
    
    fun serializeRepeatedFixed64(field: ProtoFieldInfo, values: LongArray) {
        if (values.isEmpty()) {
            return
        }
        writeStartRepeatedPrimitive(field, WireFormat.FIXED64_SIZE, values.size)
        for (value in values) {
            writeFixed64Value(value)
        }
        writeEndRepeatedPrimitive()
    }

    /** Serializes a `repeated uint64` field.  */
    
    fun serializeRepeatedUInt64(field: ProtoFieldInfo, values: LongArray) {
        if (values.isEmpty()) {
            return
        }

        var payloadSize = 0
        for (v in values) {
            payloadSize += CodedOutputStream.computeUInt64SizeNoTag(v)
        }

        writeStartRepeatedVarint(field, payloadSize)
        for (value in values) {
            writeUInt64Value(value)
        }
        writeEndRepeatedVarint()
    }

    /** Serializes a `repeated uint64` field.  */
    
    fun serializeRepeatedUInt64(field: ProtoFieldInfo, values: MutableList<Long>) {
        if (values.isEmpty()) {
            return
        }

        var payloadSize = 0
        for (v in values) {
            payloadSize += CodedOutputStream.computeUInt64SizeNoTag(v)
        }

        writeStartRepeatedVarint(field, payloadSize)
        for (value in values) {
            writeUInt64Value(value)
        }
        writeEndRepeatedVarint()
    }

    /** Serializes a `repeated int64` field.  */
    fun serializeRepeatedInt64(field: ProtoFieldInfo, values: MutableList<Long>) {
        if (values.isEmpty()) {
            return
        }

        var payloadSize = 0
        for (v in values) {
            payloadSize += CodedOutputStream.computeInt64SizeNoTag(v)
        }

        writeStartRepeatedVarint(field, payloadSize)
        for (value in values) {
            writeUInt64Value(value)
        }
        writeEndRepeatedVarint()
    }

    /** Serializes a `repeated double` field.  */
    
    fun serializeRepeatedDouble(field: ProtoFieldInfo, values: MutableList<Double>) {
        if (values.isEmpty()) {
            return
        }
        writeStartRepeatedPrimitive(field, WireFormat.FIXED64_SIZE, values.size)
        for (i in values.indices) {
            val value = values[i]
            writeDoubleValue(value)
        }
        writeEndRepeatedPrimitive()
    }

    /** Serializes `repeated message` field.  */
    
    abstract fun serializeRepeatedMessage(field: ProtoFieldInfo, repeatedMessage: Array<Marshaler>)

    /** Serializes `repeated message` field.  */
    
    abstract fun serializeRepeatedMessage(
        field: ProtoFieldInfo, repeatedMessage: MutableList<out Marshaler>)

    /**
     * Serializes `repeated message` field. This method reads elements from context, use
     * together with [StatelessMarshalerUtil.sizeRepeatedMessageWithContext].
     */
    abstract fun <T> serializeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: MutableList<out T>,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext
    )

    /**
     * Serializes `repeated message` field. This method reads elements from context, use
     * together with [StatelessMarshalerUtil.sizeRepeatedMessageWithContext].
     */
    
    fun <T> serializeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: MutableCollection<out T>,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext
    ) {
        if (messages is MutableList<*>) {
            serializeRepeatedMessageWithContext(field, messages as MutableList<T>, marshaler, context)
            return
        }

        writeStartRepeated(field)

        if (!messages.isEmpty()) {
            messages.forEach {
                writeStartRepeatedElement(field, context.size)
                marshaler.writeTo(this, it, context)
                writeEndRepeatedElement()
            }
        }

        writeEndRepeated()
    }

    /**
     * Serializes `repeated message` field. This method reads elements from context, use
     * together with [StatelessMarshalerUtil.sizeRepeatedMessageWithContext].
     */
    
    fun <K, V> serializeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: MutableMap<K, V>,
        marshaler: StatelessMarshaler2<K, V>,
        context: MarshalerContext
    ) {
        writeStartRepeated(field)
        if (!messages.isEmpty()) {
            messages.forEach { (key, value) ->
                writeStartRepeatedElement(field, context.size)
                marshaler.writeTo(this, key, value, context)
                writeEndRepeatedElement()
            }
        }
        writeEndRepeated()
    }

    /**
     * Serializes `repeated message` field. This method reads elements from context, use
     * together with [StatelessMarshalerUtil.sizeRepeatedMessageWithContext].
     */
    
    fun serializeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        attributes: Attributes,
        marshaler: StatelessMarshaler2<AttributeKey<*>, Any>,
        context: MarshalerContext) {
        writeStartRepeated(field)
        if (!attributes.isEmpty()) {
            attributes.forEach { key, value ->
                writeStartRepeatedElement(field, context.size)
                marshaler.writeTo(this, key, value, context)
                writeEndRepeatedElement()
            }
        }

        writeEndRepeated()
    }

    /** Writes start of repeated messages.  */
    abstract fun writeStartRepeated(field: ProtoFieldInfo)

    /** Writes end of repeated messages.  */
    abstract fun writeEndRepeated()

    /** Writes start of a repeated message element.  */
    abstract fun writeStartRepeatedElement(field: ProtoFieldInfo, protoMessageSize: Int)

    /** Writes end of a repeated message element.  */
    abstract fun writeEndRepeatedElement()

    /** Writes the value for a message field that has been pre-serialized.  */
    abstract fun writeSerializedMessage(protoSerialized: ByteArray, jsonSerialized: String)

    abstract override fun close()

    companion object {
        private val ATTRIBUTES_WRITER_KEY: MarshalerContext.Key = MarshalerContext.key()
    }
}
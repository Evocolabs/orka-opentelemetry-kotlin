package io.opentelemetry.kotlin.exporter.common.marshal

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo

/**
 * Kotlin Multiplatform version of StatelessMarshalerUtil for Protocol Buffers marshaling
 */
object StatelessMarshalerUtil {
    private fun <T> grouper(
        result: MutableMap<Resource, MutableMap<InstrumentationLibraryInfo, MutableList<T>>>,
        getResource: (T) -> Resource,
        getInstrumentationScope: (T) -> InstrumentationLibraryInfo,
        context: MarshalerContext
    ): (T) -> Unit = { element ->
        val resource = getResource(element)
        var scopeInfoListMap = result[resource]
        if (scopeInfoListMap == null) {
            scopeInfoListMap = context.getIdentityMap()
            result[resource] = scopeInfoListMap
        }
        val instrumentationScopeInfo = getInstrumentationScope(element)
        var elementList = scopeInfoListMap[instrumentationScopeInfo]
        if (elementList == null) {
            elementList = context.getList()
            scopeInfoListMap[instrumentationScopeInfo] = elementList
        }
        elementList.add(element)
    }

    /** Groups SDK items by resource and instrumentation scope. */
    fun <T> groupByResourceAndScope(
        dataList: Collection<T>,
        getResource: (T) -> Resource,
        getInstrumentationScope: (T) -> InstrumentationLibraryInfo,
        context: MarshalerContext
    ): Map<Resource, Map<InstrumentationLibraryInfo, List<T>>> {
        val result = context.getIdentityMap<Resource, MutableMap<InstrumentationLibraryInfo, MutableList<T>>>()

        dataList.forEach(grouper(
            result,
            getResource,
            getInstrumentationScope,
            context
        ))

        return result
    }

    /**
     * Returns the size of a string field. This method adds elements to context, use together with
     * Serializer.serializeStringWithContext.
     */
    fun sizeStringWithContext(
        field: ProtoFieldInfo,
        value: String?,
        context: MarshalerContext
    ): Int {
        if (value == null || value.isEmpty()) {
            return sizeBytes(field, 0)
        }
        return if (context.marshalStringNoAllocation()) {
            val utf8Size = getUtf8Size(value, context)
            context.addSize(utf8Size)
            sizeBytes(field, utf8Size)
        } else {
            val valueUtf8 = value.encodeToByteArray()
            context.addData(valueUtf8)
            sizeBytes(field, valueUtf8.size)
        }
    }

    /** Returns the size of a bytes field. */
    private fun sizeBytes(field: ProtoFieldInfo, length: Int): Int {
        if (length == 0) {
            return 0
        }
        return field.tagSize + CodedOutputStream.computeLengthDelimitedFieldSize(length)
    }

    /**
     * Returns the size of a repeated message field. This method adds elements to context, use
     * together with Serializer.serializeRepeatedMessageWithContext.
     */
    fun <T> sizeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: List<T>,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext
    ): Int {
        if (messages.isEmpty()) {
            return 0
        }

        var size = 0
        val fieldTagSize = field.tagSize
        for (i in messages.indices) {
            val message = messages[i]
            val sizeIndex = context.addSize()
            val fieldSize = marshaler.getBinarySerializedSize(message, context)
            context.setSize(sizeIndex, fieldSize)
            size += fieldTagSize + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize
        }
        return size
    }

    private fun <T> sizeAccumulator(
        field: ProtoFieldInfo,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext,
        totalSize: IntArray
    ): (T) -> Unit = { element ->
        val sizeIndex = context.addSize()
        val fieldSize = marshaler.getBinarySerializedSize(element, context)
        context.setSize(sizeIndex, fieldSize)
        val elementSize = field.tagSize + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize
        totalSize[0] += elementSize
    }

    private fun <K, V> pairSizeAccumulator(
        field: ProtoFieldInfo,
        marshaler: StatelessMarshaler2<K, V>,
        context: MarshalerContext,
        totalSize: IntArray
    ): (K, V) -> Unit = { key, value ->
        val sizeIndex = context.addSize()
        val fieldSize = marshaler.getBinarySerializedSize(key, value, context)
        context.setSize(sizeIndex, fieldSize)
        val elementSize = field.tagSize + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize
        totalSize[0] += elementSize
    }

    /**
     * Returns the size of a repeated message field. This method adds elements to context, use
     * together with Serializer.serializeRepeatedMessageWithContext.
     */
    fun <T> sizeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: Collection<T>,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext
    ): Int {
        if (messages is List) {
            return sizeRepeatedMessageWithContext(field, messages, marshaler, context)
        }

        if (messages.isEmpty()) {
            return 0
        }

        val totalSize = intArrayOf(0)
        val accumulator = sizeAccumulator(field, marshaler, context, totalSize)
        messages.forEach(accumulator)
        return totalSize[0]
    }

    /**
     * Returns the size of a repeated message field. This method adds elements to context, use
     * together with Serializer.serializeRepeatedMessageWithContext.
     */
    fun <K, V> sizeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        messages: Map<K, V>,
        marshaler: StatelessMarshaler2<K, V>,
        context: MarshalerContext
    ): Int {
        if (messages.isEmpty()) {
            return 0
        }

        val totalSize = intArrayOf(0)
        val accumulator = pairSizeAccumulator(field, marshaler, context, totalSize)
        messages.forEach {
            accumulator(it.key, it.value)
        }
        return totalSize[0]
    }

    /**
     * Returns the size of a repeated message field. This method adds elements to context, use
     * together with Serializer.serializeRepeatedMessageWithContext.
     */
    fun sizeRepeatedMessageWithContext(
        field: ProtoFieldInfo,
        attributes: Attributes,
        marshaler: StatelessMarshaler2<AttributeKey<*>, Any>,
        context: MarshalerContext
    ): Int {
        if (attributes.isEmpty()) {
            return 0
        }

        val totalSize = intArrayOf(0)
        val accumulator = pairSizeAccumulator(field, marshaler, context, totalSize)
        attributes.forEach(accumulator)
        return totalSize[0]
    }


    /**
     * Returns the size of a message field. This method adds elements to context, use together with
     * Serializer.serializeMessageWithContext.
     */
    fun <T> sizeMessageWithContext(
        field: ProtoFieldInfo,
        element: T,
        marshaler: StatelessMarshaler<T>,
        context: MarshalerContext
    ): Int {
        val sizeIndex = context.addSize()
        val fieldSize = marshaler.getBinarySerializedSize(element, context)
        val size = field.tagSize + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize
        context.setSize(sizeIndex, fieldSize)
        return size
    }

    /**
     * Returns the size of a message field. This method adds elements to context, use together with
     * Serializer.serializeMessageWithContext.
     */
    fun <K, V> sizeMessageWithContext(
        field: ProtoFieldInfo,
        key: K,
        value: V,
        marshaler: StatelessMarshaler2<K, V>,
        context: MarshalerContext
    ): Int {
        val sizeIndex = context.addSize()
        val fieldSize = marshaler.getBinarySerializedSize(key, value, context)
        val size = field.tagSize + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize
        context.setSize(sizeIndex, fieldSize)
        return size
    }

    /** Returns the size of utf8 encoded string in bytes. */
    private fun getUtf8Size(string: String, context: MarshalerContext): Int {
        return getUtf8Size(string, context.marshalStringUnsafe())
    }

    // Visible for testing
    internal fun getUtf8Size(string: String, useUnsafe: Boolean): Int {
        // For KMP, we'll use the safe approach since unsafe operations aren't portable
        return encodedUtf8Length(string)
    }

    // Inner loop can process at most 8 * 255 bytes without overflowing counter
    private const val MAX_INNER_LOOP_SIZE = 8 * 255
    // mask that selects only the most significant bit in every byte of the long
    private const val MOST_SIGNIFICANT_BIT_MASK = 0x80808080808080L

    /** Returns the count of bytes with negative value. */
    private fun countNegative(bytes: ByteArray): Int {
        var count = 0
        var offset = 0

        // Process 8 bytes at a time where possible
        val aligned = bytes.size and 7.inv() // bytes.size - (bytes.size % 8)
        while (offset < aligned) {
            for (i in 0 until 8) {
                if (bytes[offset + i] < 0) count++
            }
            offset += 8
        }

        // Handle remaining bytes
        for (i in offset until bytes.size) {
            if (bytes[i] < 0) count++
        }
        return count
    }

    // Adapted from Protocol Buffers UTF-8 encoding
    private fun encodedUtf8Length(string: String): Int {
        val utf16Length = string.length
        var utf8Length = utf16Length
        var i = 0

        // This loop optimizes for pure ASCII
        while (i < utf16Length && string[i].code < 0x80) {
            i++
        }

        // This loop optimizes for chars less than 0x800
        while (i < utf16Length) {
            val c = string[i]
            if (c.code < 0x800) {
                utf8Length += ((0x7f - c.code) ushr 31) // branch free!
            } else {
                utf8Length += encodedUtf8LengthGeneral(string, i)
                break
            }
            i++
        }

        if (utf8Length < utf16Length) {
            // Necessary and sufficient condition for overflow because of maximum 3x expansion
            throw IllegalArgumentException(
                "UTF-8 length does not fit in int: ${utf8Length + (1L shl 32)}"
            )
        }

        return utf8Length
    }

    private fun encodedUtf8LengthGeneral(string: String, start: Int): Int {
        val utf16Length = string.length
        var utf8Length = 0
        for (i in start until utf16Length) {
            val c = string[i]
            if (c.code < 0x800) {
                utf8Length += (0x7f - c.code) ushr 31 // branch free!
            } else {
                utf8Length += 2
                if (c.isSurrogate()) {
                    // Check that we have a well-formed surrogate pair
                    val codePoint = string.codePointAt(i)
                    if (codePoint != c.code) {
                        // Valid surrogate pair, skip the next char
                        continue
                    } else {
                        // Invalid sequence
                        // At this point we have accumulated 3 bytes of length (2 in this method and 1 in caller)
                        // for current character, reduce the length to 1 byte as we are going to encode the
                        // invalid character as ?
                        utf8Length -= 2
                    }
                }
            }
        }
        return utf8Length
    }

    /** Write utf8 encoded string to output stream. */
    internal fun writeUtf8(
        output: CodedOutputStream,
        string: String,
        utf8Length: Int,
        context: MarshalerContext
    ) {
        writeUtf8(output, string, utf8Length, context.marshalStringUnsafe())
    }

    // Visible for testing
    internal fun writeUtf8(
        output: CodedOutputStream,
        string: String,
        utf8Length: Int,
        useUnsafe: Boolean
    ) {
        // For KMP, we'll use the safe approach
        encodeUtf8(output, string)
    }

    // Encode utf8 the same way as length is computed in encodedUtf8Length
    // Adapted from Protocol Buffers UTF-8 encoding
    private fun encodeUtf8(output: CodedOutputStream, input: String) {
        val utf16Length = input.length
        var i = 0

        // Process ASCII characters first
        while (i < utf16Length) {
            val c = input[i]
            if (c.code >= 0x80) break
            output.write(c.code.toByte())
            i++
        }

        if (i == utf16Length) {
            return
        }

        // Process remaining characters
        while (i < utf16Length) {
            val c = input[i]
            when {
                c.code < 0x80 -> {
                    // 1 byte, 7 bits
                    output.write(c.code.toByte())
                }
                c.code < 0x800 -> {
                    // 11 bits, two UTF-8 bytes
                    output.write(((0xF shl 6) or (c.code ushr 6)).toByte())
                    output.write((0x80 or (0x3F and c.code)).toByte())
                }
                !c.isSurrogate() -> {
                    // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                    output.write(((0xF shl 5) or (c.code ushr 12)).toByte())
                    output.write((0x80 or (0x3F and (c.code ushr 6))).toByte())
                    output.write((0x80 or (0x3F and c.code)).toByte())
                }
                else -> {
                    // Minimum code point represented by a surrogate pair is 0x10000, 17 bits,
                    // four UTF-8 bytes
                    val codePoint = input.codePointAt(i)
                    if (codePoint != c.code) {
                        output.write(((0xF shl 4) or (codePoint ushr 18)).toByte())
                        output.write((0x80 or (0x3F and (codePoint ushr 12))).toByte())
                        output.write((0x80 or (0x3F and (codePoint ushr 6))).toByte())
                        output.write((0x80 or (0x3F and codePoint)).toByte())
                        i++ // Skip the next char as it's part of the surrogate pair
                    } else {
                        // Invalid sequence
                        output.write('?'.code.toByte())
                    }
                }
            }
            i++
        }
    }
}

// Extension function to get code point at index for String
private fun String.codePointAt(index: Int): Int {
    return if (index < length - 1) {
        val high = this[index]
        val low = this[index + 1]
        if (high.isHighSurrogate() && low.isLowSurrogate()) {
            0x10000 + ((high.code and 0x3FF) shl 10) + (low.code and 0x3FF)
        } else {
            high.code
        }
    } else {
        this[index].code
    }
}

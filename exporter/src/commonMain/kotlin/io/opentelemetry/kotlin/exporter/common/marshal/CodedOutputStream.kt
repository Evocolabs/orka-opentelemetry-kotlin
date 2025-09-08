package io.opentelemetry.kotlin.exporter.common.marshal

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.readByteArray

/**
 * Kotlin Multiplatform version of CodedOutputStream for Protocol Buffers encoding
 */
abstract class CodedOutputStream {

    companion object {
        /** The buffer size used in newInstance(Sink). */
        private val DEFAULT_BUFFER_SIZE: Int = run {
            var bufferSize = 50 * 1024
            try {
                // Platform-specific config loading would go here
                // For now, using default
            } catch (t: Throwable) {
                // Ignore
            }
            bufferSize
        }

        // Constants
        private const val MAX_VARINT_SIZE = 10
        private const val MAX_VARINT32_SIZE = 5
        private const val FIXED32_SIZE = 4
        private const val FIXED64_SIZE = 8

        /**
         * Create a new CodedOutputStream wrapping the given Sink.
         */
        fun newInstance(output: Sink): CodedOutputStream {
            return SinkEncoder(output, DEFAULT_BUFFER_SIZE)
        }

        /**
         * Create a new CodedOutputStream that writes to a byte array.
         */
        fun newInstance(output: ByteArray): CodedOutputStream {
            return ArrayEncoder(output)
        }

        // Size computation methods
        fun computeTagSize(fieldNumber: Int): Int {
            return computeUInt32SizeNoTag(makeTag(fieldNumber, 0))
        }

        fun computeInt32SizeNoTag(value: Int): Int {
            return if (value >= 0) {
                computeUInt32SizeNoTag(value)
            } else {
                // Must sign-extend
                MAX_VARINT_SIZE
            }
        }

        fun computeUInt32SizeNoTag(value: Int): Int {
            if ((value and (0.inv() shl 7)) == 0) return 1
            if ((value and (0.inv() shl 14)) == 0) return 2
            if ((value and (0.inv() shl 21)) == 0) return 3
            if ((value and (0.inv() shl 28)) == 0) return 4
            return 5
        }

        fun computeSInt32SizeNoTag(value: Int): Int {
            return computeUInt32SizeNoTag(encodeZigZag32(value))
        }

        fun computeFixed32SizeNoTag(unused: Int): Int = FIXED32_SIZE

        fun computeSFixed32SizeNoTag(unused: Int): Int = FIXED32_SIZE

        fun computeInt64SizeNoTag(value: Long): Int {
            return computeUInt64SizeNoTag(value)
        }

        fun computeUInt64SizeNoTag(value: Long): Int {
            var v = value
            // Handle two popular special cases up front
            if ((v and (0L.inv() shl 7)) == 0L) return 1
            if (v < 0L) return 10

            // Leaving us with 8 remaining, which we can divide and conquer
            var n = 2
            if ((v and (0L.inv() shl 35)) != 0L) {
                n += 4
                v = v ushr 28
            }
            if ((v and (0L.inv() shl 21)) != 0L) {
                n += 2
                v = v ushr 14
            }
            if ((v and (0L.inv() shl 14)) != 0L) {
                n += 1
            }
            return n
        }

        fun computeSInt64SizeNoTag(value: Long): Int {
            return computeUInt64SizeNoTag(encodeZigZag64(value))
        }

        fun computeFixed64SizeNoTag(unused: Long): Int = FIXED64_SIZE

        fun computeSFixed64SizeNoTag(unused: Long): Int = FIXED64_SIZE

        fun computeFloatSizeNoTag(unused: Float): Int = FIXED32_SIZE

        fun computeDoubleSizeNoTag(unused: Double): Int = FIXED64_SIZE

        fun computeBoolSizeNoTag(unused: Boolean): Int = 1

        fun computeEnumSizeNoTag(value: Int): Int {
            return computeInt32SizeNoTag(value)
        }

        fun computeByteArraySizeNoTag(value: ByteArray): Int {
            return computeLengthDelimitedFieldSize(value.size)
        }

        fun computeLengthDelimitedFieldSize(fieldLength: Int): Int {
            return computeUInt32SizeNoTag(fieldLength) + fieldLength
        }

        /**
         * Encode a ZigZag-encoded 32-bit value.
         */
        fun encodeZigZag32(n: Int): Int {
            // Note: the right-shift must be arithmetic
            return (n shl 1) xor (n shr 31)
        }

        /**
         * Encode a ZigZag-encoded 64-bit value.
         */
        fun encodeZigZag64(n: Long): Long {
            // Note: the right-shift must be arithmetic
            return (n shl 1) xor (n shr 63)
        }

        private fun makeTag(fieldNumber: Int, wireType: Int): Int {
            return (fieldNumber shl 3) or wireType
        }
    }

    // Disallow construction outside of this class
    protected constructor()

    /** Write an array of bytes. */
    fun writeRawBytes(value: ByteArray) {
        write(value, 0, value.size)
    }

    // Abstract methods that subclasses must implement
    abstract fun writeInt32NoTag(value: Int)
    abstract fun writeUInt32NoTag(value: Int)
    abstract fun writeFixed32NoTag(value: Int)
    abstract fun writeUInt64NoTag(value: Long)
    abstract fun writeFixed64NoTag(value: Long)

    // Concrete implementations using the abstract methods
    fun writeSInt32NoTag(value: Int) {
        writeUInt32NoTag(encodeZigZag32(value))
    }

    fun writeSFixed32NoTag(value: Int) {
        writeFixed32NoTag(value)
    }

    fun writeInt64NoTag(value: Long) {
        writeUInt64NoTag(value)
    }

    fun writeSInt64NoTag(value: Long) {
        writeUInt64NoTag(encodeZigZag64(value))
    }

    fun writeSFixed64NoTag(value: Long) {
        writeFixed64NoTag(value)
    }

    fun writeFloatNoTag(value: Float) {
        writeFixed32NoTag(value.toBits())
    }

    fun writeDoubleNoTag(value: Double) {
        writeFixed64NoTag(value.toBits())
    }

    fun writeBoolNoTag(value: Boolean) {
        write((if (value) 1 else 0).toByte())
    }

    fun writeEnumNoTag(value: Int) {
        writeInt32NoTag(value)
    }

    fun writeByteArrayNoTag(value: ByteArray) {
        writeByteArrayNoTag(value, 0, value.size)
    }

    // Abstract methods for low-level writing
    abstract fun write(value: Byte)
    abstract fun write(value: ByteArray, offset: Int, length: Int)
    abstract fun writeByteArrayNoTag(value: ByteArray, offset: Int, length: Int)
    abstract fun writeByteBufferNoTag(value: Buffer)

    abstract fun flush()

    /**
     * Abstract base class for buffered encoders.
     */
    abstract class AbstractBufferedEncoder(bufferSize: Int) : CodedOutputStream() {
        protected val buffer = ByteArray(bufferSize)
        protected val limit = buffer.size
        protected var position = 0
        protected var totalBytesWritten = 0

        /**
         * This method does not perform bounds checking on the array.
         */
        protected fun buffer(value: Byte) {
            buffer[position++] = value
            totalBytesWritten++
        }

        /**
         * This method does not perform bounds checking on the array.
         */
        protected fun bufferUInt32NoTag(value: Int) {
            var v = value
            while (true) {
                if ((v and 0x7F.inv()) == 0) {
                    buffer[position++] = v.toByte()
                    totalBytesWritten++
                    return
                } else {
                    buffer[position++] = ((v and 0x7F) or 0x80).toByte()
                    totalBytesWritten++
                    v = v ushr 7
                }
            }
        }

        /**
         * This method does not perform bounds checking on the array.
         */
        protected fun bufferUInt64NoTag(value: Long) {
            var v = value
            while (true) {
                if ((v and 0x7FL.inv()) == 0L) {
                    buffer[position++] = v.toByte()
                    totalBytesWritten++
                    return
                } else {
                    buffer[position++] = ((v.toInt() and 0x7F) or 0x80).toByte()
                    totalBytesWritten++
                    v = v ushr 7
                }
            }
        }

        /**
         * This method does not perform bounds checking on the array.
         */
        protected fun bufferFixed32NoTag(value: Int) {
            buffer[position++] = (value and 0xFF).toByte()
            buffer[position++] = ((value shr 8) and 0xFF).toByte()
            buffer[position++] = ((value shr 16) and 0xFF).toByte()
            buffer[position++] = ((value shr 24) and 0xFF).toByte()
            totalBytesWritten += FIXED32_SIZE
        }

        /**
         * This method does not perform bounds checking on the array.
         */
        protected fun bufferFixed64NoTag(value: Long) {
            buffer[position++] = (value and 0xFF).toByte()
            buffer[position++] = ((value shr 8) and 0xFF).toByte()
            buffer[position++] = ((value shr 16) and 0xFF).toByte()
            buffer[position++] = ((value shr 24) and 0xFF).toByte()
            buffer[position++] = ((value shr 32) and 0xFF).toByte()
            buffer[position++] = ((value shr 40) and 0xFF).toByte()
            buffer[position++] = ((value shr 48) and 0xFF).toByte()
            buffer[position++] = ((value shr 56) and 0xFF).toByte()
            totalBytesWritten += FIXED64_SIZE
        }
    }

    /**
     * A CodedOutputStream that writes to a Sink with internal buffering.
     */
    private class SinkEncoder(
        private var sink: Sink,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ) : AbstractBufferedEncoder(bufferSize) {

        fun reset(sink: Sink) {
            this.sink = sink
            position = 0
            totalBytesWritten = 0
        }

        override fun writeByteArrayNoTag(value: ByteArray, offset: Int, length: Int) {
            writeUInt32NoTag(length)
            write(value, offset, length)
        }

        override fun writeByteBufferNoTag(value: Buffer) {
            writeUInt32NoTag(value.size.toInt())
            write(value.readByteArray(), 0, value.size.toInt())
        }

        override fun write(value: Byte) {
            if (position == limit) {
                doFlush()
            }
            buffer(value)
        }

        override fun writeInt32NoTag(value: Int) {
            if (value >= 0) {
                writeUInt32NoTag(value)
            } else {
                // Must sign-extend
                writeUInt64NoTag(value.toLong())
            }
        }

        override fun writeUInt32NoTag(value: Int) {
            flushIfNotAvailable(MAX_VARINT32_SIZE)
            bufferUInt32NoTag(value)
        }

        override fun writeFixed32NoTag(value: Int) {
            flushIfNotAvailable(FIXED32_SIZE)
            bufferFixed32NoTag(value)
        }

        override fun writeUInt64NoTag(value: Long) {
            flushIfNotAvailable(MAX_VARINT_SIZE)
            bufferUInt64NoTag(value)
        }

        override fun writeFixed64NoTag(value: Long) {
            flushIfNotAvailable(FIXED64_SIZE)
            bufferFixed64NoTag(value)
        }

        override fun flush() {
            if (position > 0) {
                doFlush()
            }
        }

        override fun write(value: ByteArray, offset: Int, length: Int) {
            if (limit - position >= length) {
                // We have room in the current buffer
                value.copyInto(buffer, position, offset, offset + length)
                position += length
                totalBytesWritten += length
            } else {
                // Write extends past current buffer
                val bytesWritten = limit - position
                value.copyInto(buffer, position, offset, offset + bytesWritten)
                val newOffset = offset + bytesWritten
                val newLength = length - bytesWritten
                position = limit
                totalBytesWritten += bytesWritten
                doFlush()

                // Now deal with the rest
                if (newLength <= limit) {
                    // Fits in new buffer
                    value.copyInto(buffer, 0, newOffset, newOffset + newLength)
                    position = newLength
                } else {
                    // Write is very big, do it all at once
                    sink.write(value, newOffset, newOffset + newLength)
                }
                totalBytesWritten += newLength
            }
        }

        private fun flushIfNotAvailable(requiredSize: Int) {
            if (limit - position < requiredSize) {
                doFlush()
            }
        }

        private fun doFlush() {
            sink.write(buffer, 0, position)
            position = 0
        }
    }

    /**
     * A CodedOutputStream that writes to a byte array.
     */
    private class ArrayEncoder(private val output: ByteArray) : CodedOutputStream() {
        private var position = 0

        override fun writeInt32NoTag(value: Int) {
            if (value >= 0) {
                writeUInt32NoTag(value)
            } else {
                writeUInt64NoTag(value.toLong())
            }
        }

        override fun writeUInt32NoTag(value: Int) {
            var v = value
            while (true) {
                if ((v and 0x7F.inv()) == 0) {
                    output[position++] = v.toByte()
                    return
                } else {
                    output[position++] = ((v and 0x7F) or 0x80).toByte()
                    v = v ushr 7
                }
            }
        }

        override fun writeFixed32NoTag(value: Int) {
            output[position++] = (value and 0xFF).toByte()
            output[position++] = ((value shr 8) and 0xFF).toByte()
            output[position++] = ((value shr 16) and 0xFF).toByte()
            output[position++] = ((value shr 24) and 0xFF).toByte()
        }

        override fun writeUInt64NoTag(value: Long) {
            var v = value
            while (true) {
                if ((v and 0x7FL.inv()) == 0L) {
                    output[position++] = v.toByte()
                    return
                } else {
                    output[position++] = ((v.toInt() and 0x7F) or 0x80).toByte()
                    v = v ushr 7
                }
            }
        }

        override fun writeFixed64NoTag(value: Long) {
            output[position++] = (value and 0xFF).toByte()
            output[position++] = ((value shr 8) and 0xFF).toByte()
            output[position++] = ((value shr 16) and 0xFF).toByte()
            output[position++] = ((value shr 24) and 0xFF).toByte()
            output[position++] = ((value shr 32) and 0xFF).toByte()
            output[position++] = ((value shr 40) and 0xFF).toByte()
            output[position++] = ((value shr 48) and 0xFF).toByte()
            output[position++] = ((value shr 56) and 0xFF).toByte()
        }

        override fun write(value: Byte) {
            output[position++] = value
        }

        override fun write(value: ByteArray, offset: Int, length: Int) {
            value.copyInto(output, position, offset, offset + length)
            position += length
        }

        override fun writeByteArrayNoTag(value: ByteArray, offset: Int, length: Int) {
            writeUInt32NoTag(length)
            write(value, offset, length)
        }

        override fun writeByteBufferNoTag(value: Buffer) {
            writeUInt32NoTag(value.size.toInt())
            write(value.readByteArray(), 0, value.size.toInt())
        }

        override fun flush() {
            // No-op for array encoder
        }

        fun toByteArray(): ByteArray {
            return output.copyOf(position)
        }
    }
}
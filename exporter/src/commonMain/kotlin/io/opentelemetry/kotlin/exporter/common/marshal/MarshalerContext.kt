package io.opentelemetry.kotlin.exporter.common.marshal

import io.opentelemetry.kotlin.api.trace.SpanId
import io.opentelemetry.kotlin.api.trace.TraceId
import kotlinx.atomicfu.atomic
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Class for keeping marshaling state. The state consists of integers, that we call sizes, and
 * objects, that we call data. Both integers and objects can be read from the state in the order
 * they were added (first in, first out). Additionally, this class provides various pools and caches
 * for objects that can be reused between marshalling attempts.
 *
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
class MarshalerContext(
    private val marshalStringNoAllocation: Boolean = true,
    private val marshalStringUnsafe: Boolean = true
) {
    private var sizes = IntArray(16)
    private var sizeReadIndex = 0
    private var sizeWriteIndex = 0
    private var data = arrayOfNulls<Any>(16)
    private var dataReadIndex = 0
    private var dataWriteIndex = 0

    fun marshalStringNoAllocation(): Boolean {
        return marshalStringNoAllocation
    }

    fun marshalStringUnsafe(): Boolean {
        return marshalStringUnsafe
    }

    fun addSize(size: Int) {
        growSizeIfNeeded()
        sizes[sizeWriteIndex++] = size
    }

    fun addSize(): Int {
        growSizeIfNeeded()
        return sizeWriteIndex++
    }

    private fun growSizeIfNeeded() {
        if (sizeWriteIndex == sizes.size) {
            val newSizes = IntArray(sizes.size * 2)
            sizes.copyInto(newSizes, 0, 0, sizes.size)
            sizes = newSizes
        }
    }

    fun setSize(index: Int, size: Int) {
        sizes[index] = size
    }

    val size: Int
        get() = sizes[sizeReadIndex++]

    fun addData(o: Any?) {
        growDataIfNeeded()
        data[dataWriteIndex++] = o
    }

    private fun growDataIfNeeded() {
        if (dataWriteIndex == data.size) {
            val newData = arrayOfNulls<Any>(data.size * 2)
            data.copyInto(newData, 0, 0, data.size)
            data = newData
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getData(type: KClass<out T>): T =
         type.cast(data[dataReadIndex++])


    private val traceIdPool = IdPool(TraceId.length / 2)

    val traceIdBuffer: ByteArray
        /** Returns a buffer that can be used to hold a trace id.  */
        get() = traceIdPool.get()

    private val spanIdPool = IdPool(SpanId.length / 2)

    val spanIdBuffer: ByteArray
        /** Returns a buffer that can be used to hold a span id.  */
        get() = spanIdPool.get()

    private class IdPool(val idSize: Int) {
        private val pool: MutableList<ByteArray> = mutableListOf()
        var index: Int = 0

        fun get(): ByteArray {
            if (index < pool.size) {
                return pool[index++]
            }
            val result = ByteArray(idSize)
            pool.add(result)
            index++

            return result
        }

        fun reset() {
            index = 0
        }
    }

    private val mapPool = Pool<MutableMap<Any, Any>>(
        factory = { mutableMapOf() },
        clean = { it.clear() }
    )

    /** Returns a pooled map.  */
    @Suppress("UNCHECKED_CAST")
    fun <K, V> getIdentityMap(): MutableMap<K, V> {
        return mapPool.get() as MutableMap<K, V>
    }

    private val listPool = Pool<MutableList<Any?>>(
        factory = { mutableListOf() },
        clean = { it.clear() }
    )

    /** Returns a pooled list.  */
    @Suppress("UNCHECKED_CAST")
    fun <T> getList(): MutableList<T> {
        return listPool.get() as MutableList<T>
    }

    private class Pool<T>(
        private val factory: () -> T,
        private val clean: (T) -> Unit
    ) {
        private val pool: MutableList<T> = mutableListOf()
        private var index = 0

        fun get(): T {
            if (index < pool.size) {
                return pool[index++]
            }
            val result: T = factory()
            pool.add(result)
            index++

            return result
        }

        fun reset() {
            for (i in 0 until index) {
                clean(pool[i])
            }
            index = 0
        }
    }

    /** Reset context so that serialization could be re-run.  */
    fun resetReadIndex() {
        sizeReadIndex = 0
        dataReadIndex = 0
    }

    /** Reset context so that it could be reused.  */
    fun reset() {
        sizeReadIndex = 0
        sizeWriteIndex = 0
        for (i in 0 until dataWriteIndex) {
            data[i] = null
        }
        dataReadIndex = 0
        dataWriteIndex = 0

        traceIdPool.reset()
        spanIdPool.reset()

        mapPool.reset()
        listPool.reset()
    }

    /**
     * This class is internal and is hence not for public use. Its APIs are unstable and can change at
     * any time.
     */
    class Key {
        val index: Int = KEY_INDEX.getAndIncrement()
    }

    private var instances = arrayOfNulls<Any>(16)

    fun <T> getInstance(key: Key, supplier: () -> T): T {
        if (key.index >= instances.size) {
            val newData = arrayOfNulls<Any>(instances.size * 2)
            instances.copyInto(newData, 0, 0, instances.size)
            instances = newData
        }

        @Suppress("UNCHECKED_CAST")
        var result = instances[key.index] as T
        if (result == null) {
            result = supplier()
            instances[key.index] = result
        }
        return result
    }

    companion object {
        private val KEY_INDEX = atomic(0)

        fun key(): Key {
            return Key()
        }
    }
}
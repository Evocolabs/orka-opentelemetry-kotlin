package io.opentelemetry.kotlin.exporter.common.marshal

import io.opentelemetry.kotlin.api.trace.SpanId
import io.opentelemetry.kotlin.api.trace.TraceId
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.resources.Resource

object MarshalerUtil {
    private val TRACE_ID_VALUE_SIZE = CodedOutputStream.computeLengthDelimitedFieldSize(TraceId.length / 2)

    private val SPAN_ID_VALUE_SIZE = CodedOutputStream.computeLengthDelimitedFieldSize(SpanId.length / 2)

    fun<T, U> groupByResourceAndScope(
        dataList: Collection<T>,
        getResource: (T) -> Resource,
        getLibraryInfo: (T) -> InstrumentationLibraryInfo,
        createMarshaler: (T) -> U
    ): MutableMap<Resource, MutableMap<InstrumentationLibraryInfo, MutableList<U>>> {
        val result = mutableMapOf<Resource, MutableMap<InstrumentationLibraryInfo, MutableList<U>>>()

        for (data in dataList) {
            val libraryInfoListMap = result.getOrPut(getResource(data), { mutableMapOf() })
            val marshalerList = libraryInfoListMap.getOrPut(getLibraryInfo(data), { mutableListOf() })
            marshalerList.add(createMarshaler(data))
        }
        return result
    }

    fun sizeBytes(field: ProtoFieldInfo, message: ByteArray): Int {
        if (message.isEmpty()) return 0
        return field.tagSize + CodedOutputStream.computeByteArraySizeNoTag(message)
    }
}
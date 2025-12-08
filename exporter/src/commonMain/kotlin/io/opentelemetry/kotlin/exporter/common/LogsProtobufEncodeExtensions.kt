package io.opentelemetry.kotlin.exporter.common

import io.opentelemetry.kotlin.api.common.Value
import io.opentelemetry.kotlin.api.common.ValueArray
import io.opentelemetry.kotlin.api.common.ValueBoolean
import io.opentelemetry.kotlin.api.common.ValueBytes
import io.opentelemetry.kotlin.api.common.ValueDouble
import io.opentelemetry.kotlin.api.common.ValueLong
import io.opentelemetry.kotlin.api.common.ValueMap
import io.opentelemetry.kotlin.api.common.ValueString
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.common.v1.AnyValue
import io.opentelemetry.proto.common.v1.ArrayValue
import io.opentelemetry.proto.common.v1.KeyValueList
import io.opentelemetry.proto.logs.v1.LogRecord as PbLogRecord
import io.opentelemetry.proto.logs.v1.ResourceLogs
import io.opentelemetry.proto.logs.v1.ScopeLogs
import io.opentelemetry.proto.logs.v1.SeverityNumber
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString

/**
 * Encodes a Value<*> to protobuf AnyValue.
 */
fun Value<*>.encodeAsAnyValue(): AnyValue {
    return when (this) {
        is ValueString -> AnyValue(string_value = this.value)
        is ValueBoolean -> AnyValue(bool_value = this.value)
        is ValueLong -> AnyValue(int_value = this.value)
        is ValueDouble -> AnyValue(double_value = this.value)
        is ValueBytes -> AnyValue(bytes_value = this.value.toByteString())
        is ValueArray<*> -> AnyValue(
            array_value = ArrayValue(
                values = this.value.map { it.encodeAsAnyValue() }
            )
        )
        is ValueMap -> AnyValue(
            kvlist_value = KeyValueList(
                values = this.value.map { (key, value) ->
                    encodeKeyValue(key, value)
                }
            )
        )
        else -> throw IllegalArgumentException("Unsupported Value type: ${this::class}")
    }
}

/**
 * Encodes severity number to protobuf SeverityNumber enum.
 */
fun Int.encodeSeverityNumber(): SeverityNumber {
    return when (this) {
        0 -> SeverityNumber.SEVERITY_NUMBER_UNSPECIFIED
        1 -> SeverityNumber.SEVERITY_NUMBER_TRACE
        2 -> SeverityNumber.SEVERITY_NUMBER_TRACE2
        3 -> SeverityNumber.SEVERITY_NUMBER_TRACE3
        4 -> SeverityNumber.SEVERITY_NUMBER_TRACE4
        5 -> SeverityNumber.SEVERITY_NUMBER_DEBUG
        6 -> SeverityNumber.SEVERITY_NUMBER_DEBUG2
        7 -> SeverityNumber.SEVERITY_NUMBER_DEBUG3
        8 -> SeverityNumber.SEVERITY_NUMBER_DEBUG4
        9 -> SeverityNumber.SEVERITY_NUMBER_INFO
        10 -> SeverityNumber.SEVERITY_NUMBER_INFO2
        11 -> SeverityNumber.SEVERITY_NUMBER_INFO3
        12 -> SeverityNumber.SEVERITY_NUMBER_INFO4
        13 -> SeverityNumber.SEVERITY_NUMBER_WARN
        14 -> SeverityNumber.SEVERITY_NUMBER_WARN2
        15 -> SeverityNumber.SEVERITY_NUMBER_WARN3
        16 -> SeverityNumber.SEVERITY_NUMBER_WARN4
        17 -> SeverityNumber.SEVERITY_NUMBER_ERROR
        18 -> SeverityNumber.SEVERITY_NUMBER_ERROR2
        19 -> SeverityNumber.SEVERITY_NUMBER_ERROR3
        20 -> SeverityNumber.SEVERITY_NUMBER_ERROR4
        21 -> SeverityNumber.SEVERITY_NUMBER_FATAL
        22 -> SeverityNumber.SEVERITY_NUMBER_FATAL2
        23 -> SeverityNumber.SEVERITY_NUMBER_FATAL3
        24 -> SeverityNumber.SEVERITY_NUMBER_FATAL4
        else -> SeverityNumber.SEVERITY_NUMBER_UNSPECIFIED
    }
}

/**
 * Encodes a single LogRecordData to protobuf LogRecord.
 */
fun LogRecordData.encode(): PbLogRecord {
    val traceIdBytes = if (spanContext.isValid()) {
        spanContext.traceId.decodeHex()
    } else {
        ByteString.EMPTY
    }

    val spanIdBytes = if (spanContext.isValid()) {
        spanContext.spanId.decodeHex()
    } else {
        ByteString.EMPTY
    }

    val flags = if (spanContext.isValid()) {
        spanContext.traceFlags.toByte().toInt()
    } else {
        0
    }

    return PbLogRecord(
        time_unix_nano = timestampEpochNanos,
        observed_time_unix_nano = observedTimestampEpochNanos,
        severity_number = severityNumber.encodeSeverityNumber(),
        severity_text = severityText ?: "",
        body = body?.encodeAsAnyValue(),
        attributes = attributes.encode(),
        dropped_attributes_count = (totalAttributeCount - attributes.size).toUInt(),
        flags = flags.toUInt(),
        trace_id = traceIdBytes,
        span_id = spanIdBytes
    )
}

/**
 * Encodes a collection of LogRecordData to ExportLogsServiceRequest.
 * Groups logs by Resource, then by InstrumentationScope.
 */
fun <T : LogRecordData> Collection<T>.encode(): ExportLogsServiceRequest {
    val sdkResourceLogs: MutableMap<Resource, MutableMap<InstrumentationLibraryInfo, MutableList<PbLogRecord>>> = mutableMapOf()

    forEach {
        val resource = it.resource
        val instrumentationLibrary = it.instrumentationLibraryInfo
        val pbLogRecord = it.encode()

        if (!sdkResourceLogs.containsKey(resource)) {
            sdkResourceLogs[resource] = mutableMapOf()
        }

        if (!sdkResourceLogs[resource]!!.containsKey(instrumentationLibrary)) {
            sdkResourceLogs[resource]!![instrumentationLibrary] = mutableListOf()
        }

        sdkResourceLogs[resource]!![instrumentationLibrary]!!.add(pbLogRecord)
    }

    val resourceLogs = sdkResourceLogs.map { (resource, instrumentationLibraries) ->
        val scopeLogs = instrumentationLibraries.map { (instrumentationLibrary, logRecords) ->
            ScopeLogs(
                scope = instrumentationLibrary.encode(),
                log_records = logRecords,
                schema_url = instrumentationLibrary.schemaUrl ?: ""
            )
        }
        ResourceLogs(
            resource = resource.encode(),
            scope_logs = scopeLogs,
            schema_url = resource.schemaUrl ?: "",
        )
    }

    return ExportLogsServiceRequest(
        resource_logs = resourceLogs
    )
}

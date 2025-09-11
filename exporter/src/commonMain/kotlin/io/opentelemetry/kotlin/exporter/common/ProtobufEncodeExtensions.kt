package io.opentelemetry.kotlin.exporter.common

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.api.trace.SpanKind
import io.opentelemetry.kotlin.api.trace.TraceState
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.sdk.trace.data.SpanData
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.common.v1.AnyValue
import io.opentelemetry.proto.common.v1.ArrayValue
import io.opentelemetry.proto.common.v1.InstrumentationScope
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.common.v1.KeyValueList
import io.opentelemetry.proto.trace.v1.ResourceSpans
import io.opentelemetry.proto.trace.v1.ScopeSpans
import io.opentelemetry.proto.trace.v1.SpanFlags
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import io.opentelemetry.proto.resource.v1.Resource as PbResource
import io.opentelemetry.proto.trace.v1.Span as PbSpan

fun TraceState.encode(): String {
    val states = mutableListOf<String>()
    this.forEach { key, value ->
        states.add("$key=$value")
    }
    return states.joinToString(",")
}

fun SpanContext.encodeFlags(): Int {
    var flags = SpanFlags.SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK.value
    if (isRemote) {
        flags = flags or SpanFlags.SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK.value
    }
    return flags
}

fun SpanKind.encode(): PbSpan.SpanKind {
    return when (this) {
        SpanKind.INTERNAL -> PbSpan.SpanKind.SPAN_KIND_INTERNAL
        SpanKind.SERVER -> PbSpan.SpanKind.SPAN_KIND_SERVER
        SpanKind.CLIENT -> PbSpan.SpanKind.SPAN_KIND_CLIENT
        SpanKind.PRODUCER -> PbSpan.SpanKind.SPAN_KIND_PRODUCER
        SpanKind.CONSUMER -> PbSpan.SpanKind.SPAN_KIND_CONSUMER
    }
}

fun Attributes.encode(): List<KeyValue> {
    val keyValues = mutableListOf<KeyValue>()
    this.forEach { key, value ->
        keyValues.add(KeyValue(key = key.key, value_ = encodeValue(value)))
    }
    return keyValues
}

fun encodeKeyValue(key: String, value: Any): KeyValue {
    return KeyValue(key = key, value_ = encodeValue(value))
}

fun encodeValue(value: Any): AnyValue =
    when (value) {
        is String -> AnyValue(string_value = value)
        is Boolean -> AnyValue(bool_value = value)
        is Long -> AnyValue(int_value = value)
        is Double -> AnyValue(double_value = value)
        is Float -> AnyValue(double_value = value.toDouble())
        is ByteArray -> AnyValue(bytes_value = value.toByteString())
        is Collection<*> -> AnyValue(
            array_value = ArrayValue(
                value.mapNotNull { if (it != null) encodeValue(it) else null }
            )
        )

        is Map<*, *> -> AnyValue(
            kvlist_value = KeyValueList(
                value.mapNotNull { if (it.key != null && it.value != null) encodeKeyValue(it.key.toString(), it.value!!) else null }
            )
        )

        else -> throw IllegalArgumentException("Unsupported value type: $value")
    }


fun SpanData.encode(): PbSpan = PbSpan(
    trace_id = traceId.decodeHex(),
    span_id = spanId.decodeHex(),
    trace_state = spanContext.traceState.encode(),
    parent_span_id = parentSpanId?.decodeHex() ?: ByteString.EMPTY,
    flags = spanContext.encodeFlags(),
    name = name,
    kind = kind.encode(),
    start_time_unix_nano = startEpochNanos,
    end_time_unix_nano = endEpochNanos,
    attributes = attributes.encode(),
    dropped_attributes_count = totalAttributeCount - attributes.size,
    events = events.map {
        PbSpan.Event(
            time_unix_nano = it.epochNanos,
            name = it.name,
            attributes = it.attributes.encode(),
            dropped_attributes_count = it.droppedAttributesCount,
        )
    },
    dropped_events_count = totalRecordedEvents - events.size,
    links = links.map {
        PbSpan.Link(
            trace_id = traceId.decodeHex(),
            span_id = spanId.decodeHex(),
            trace_state = spanContext.traceState.encode(),
            attributes = it.attributes.encode(),
            flags = it.spanContext.encodeFlags()
        )
    },
    dropped_links_count = totalRecordedLinks - links.size
)

fun <T : SpanData> Collection<T>.encode(): ExportTraceServiceRequest {
    val sdkResourceSpans: MutableMap<Resource, MutableMap<InstrumentationLibraryInfo, MutableList<PbSpan>>> = mutableMapOf()

    forEach {
        val resource = it.resource
        val instrumentationLibrary = it.instrumentationLibraryInfo
        val pbSpan = it.encode()

        if (!sdkResourceSpans.containsKey(resource)) {
            sdkResourceSpans[resource] = mutableMapOf()
        }

        if (!sdkResourceSpans[resource]!!.containsKey(instrumentationLibrary)) {
            sdkResourceSpans[resource]!![instrumentationLibrary] = mutableListOf()
        }

        sdkResourceSpans[resource]!![instrumentationLibrary]!!.add(pbSpan)
    }

    val resourceSpans = sdkResourceSpans.map { (resource, instrumentationLibraries) ->
        val scopeSpans = instrumentationLibraries.map { (instrumentationLibrary, spans) ->
            ScopeSpans(
                scope = instrumentationLibrary.encode(),
                spans = spans,
                schema_url = instrumentationLibrary.schemaUrl ?: ""
            )
        }
        ResourceSpans(
            resource = resource.encode(),
            scope_spans = scopeSpans,
            schema_url = resource.schemaUrl ?: "",
        )
    }
    return ExportTraceServiceRequest(
        resource_spans = resourceSpans
    )
}

fun Resource.encode(): PbResource =
    PbResource(
        attributes = attributes.encode()
    )


fun InstrumentationLibraryInfo.encode(): InstrumentationScope =
    InstrumentationScope(
        name = name,
        version = version ?: ""
    )


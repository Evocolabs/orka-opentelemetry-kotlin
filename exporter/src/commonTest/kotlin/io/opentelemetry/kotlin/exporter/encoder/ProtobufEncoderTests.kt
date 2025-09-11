package io.opentelemetry.kotlin.exporter.encoder

import io.kotest.assertions.shouldFail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.opentelemetry.kotlin.api.common.AttributeType
import io.opentelemetry.kotlin.api.trace.SpanKind
import io.opentelemetry.kotlin.api.trace.StatusCode
import io.opentelemetry.kotlin.exporter.common.encode
import io.opentelemetry.kotlin.exporter.common.encodeFlags
import io.opentelemetry.kotlin.exporter.common.encodeKeyValue
import io.opentelemetry.kotlin.exporter.common.encodeValue
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.sdk.testing.trace.TestAttributeKey
import io.opentelemetry.kotlin.sdk.testing.trace.TestAttributes
import io.opentelemetry.kotlin.sdk.testing.trace.TestEventData
import io.opentelemetry.kotlin.sdk.testing.trace.TestInstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.testing.trace.TestLinkData
import io.opentelemetry.kotlin.sdk.testing.trace.TestSpanContext
import io.opentelemetry.kotlin.sdk.testing.trace.TestSpanData
import io.opentelemetry.kotlin.sdk.testing.trace.TestTraceState
import io.opentelemetry.kotlin.sdk.trace.data.StatusData
import io.opentelemetry.proto.trace.v1.SpanFlags
import io.opentelemetry.proto.trace.v1.Status
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import kotlin.test.*
import io.opentelemetry.proto.trace.v1.Span as PbSpan

class ExporterExtensionsTest {
    @Test
    fun `TraceState encode returns empty string for empty trace state`() {
        val traceState = TestTraceState(emptyMap())

        val result = traceState.encode()

        result shouldBe ""
    }

    @Test
    fun `TraceState encode returns single key-value pair`() {
        val traceState = TestTraceState(mapOf("key1" to "value1"))

        val result = traceState.encode()

        result shouldBe "key1=value1"
    }

    @Test
    fun `TraceState encode returns multiple key-value pairs joined by comma`() {
        val traceState = TestTraceState(mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        ))

        val result = traceState.encode()

        result shouldBe "key1=value1,key2=value2,key3=value3"
    }

    @Test
    fun `SpanContext encodeFlags returns remote flag when isRemote is true`() {
        val spanContext = TestSpanContext(
            isRemote = true
        )

        val result = spanContext.encodeFlags()

        val expectedFlags = SpanFlags.SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK.value or
            SpanFlags.SPAN_FLAGS_CONTEXT_IS_REMOTE_MASK.value

        result shouldBe expectedFlags
    }

    @Test
    fun `SpanContext encodeFlags returns only has remote flag when isRemote is false`() {
        val spanContext = TestSpanContext(isRemote = false)

        val result = spanContext.encodeFlags()

        result shouldBe SpanFlags.SPAN_FLAGS_CONTEXT_HAS_IS_REMOTE_MASK.value
    }

    @Test
    fun `SpanKind encode maps all span kinds correctly`() {
        SpanKind.INTERNAL.encode() shouldBe PbSpan.SpanKind.SPAN_KIND_INTERNAL
        SpanKind.SERVER.encode() shouldBe PbSpan.SpanKind.SPAN_KIND_SERVER
        SpanKind.CLIENT.encode() shouldBe PbSpan.SpanKind.SPAN_KIND_CLIENT
        SpanKind.PRODUCER.encode() shouldBe PbSpan.SpanKind.SPAN_KIND_PRODUCER
        SpanKind.CONSUMER.encode() shouldBe PbSpan.SpanKind.SPAN_KIND_CONSUMER
    }

    @Test
    fun `Attributes encode returns empty list for empty attributes`() {
        val attributes = TestAttributes(emptyMap())

        val result = attributes.encode()

        result should { it.isEmpty() }
    }

    @Test
    fun `Attributes encode converts attributes to key-value pairs`() {
        val stringKey = TestAttributeKey<String>("string_attr", AttributeType.STRING)
        val longKey = TestAttributeKey<Long>("long_attr", AttributeType.LONG)
        val attributes = TestAttributes(mapOf(
            stringKey to "test_value",
            longKey to 42L
        ))

        val result = attributes.encode()

        result.size shouldBe 2
        result[0].key shouldBe "string_attr"
        result[0].value_?.string_value shouldBe "test_value"
        result[1].key shouldBe "long_attr"
        result[1].value_?.int_value shouldBe 42
    }

    @Test
    fun `encodeKeyValue creates KeyValue with correct key and encoded value`() {
        val result = encodeKeyValue("test_key", "test_value")

        result.key shouldBe "test_key"
        result.value_?.string_value shouldBe "test_value"
    }

    @Test
    fun `encodeValue handles String values`() {
        val result = encodeValue("test_string")

        result.string_value shouldBe "test_string"
        result.bool_value shouldBe null
        result.int_value shouldBe null
        result.double_value shouldBe null
    }

    @Test
    fun `encodeValue handles Boolean values`() {
        val trueResult = encodeValue(true)
        val falseResult = encodeValue(false)

        trueResult.bool_value shouldBe true
        falseResult.bool_value shouldBe false
        trueResult.string_value shouldBe null
        falseResult.string_value shouldBe null
    }

    @Test
    fun `encodeValue handles Long values`() {
        val result = encodeValue(123L)

        result.int_value shouldBe 123
        result.string_value shouldBe null
        result.double_value shouldBe null
    }

    @Test
    fun `encodeValue handles Double values`() {
        val result = encodeValue(123.45)

        result.double_value shouldBe 123.45
        result.string_value shouldBe null
        result.int_value shouldBe null
    }

    @Test
    fun `encodeValue handles Float values by converting to Double`() {
        val value = 123.45
        val result = encodeValue(value)

        val delta = 0.0001
        result.double_value?.shouldBeGreaterThan(value - delta)
        result.double_value?.shouldBeLessThan(value + delta)
        result.string_value shouldBe null
    }

    @Test
    fun `encodeValue handles ByteArray values`() {
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val result = encodeValue(byteArray)

        result.bytes_value shouldBe byteArray.toByteString()
        result.string_value shouldBe null
    }

    @Test
    fun `encodeValue handles Collection values`() {
        val collection = listOf("item1", 42L, true)
        val result = encodeValue(collection)

        result.array_value shouldNotBe null
        result.array_value?.values?.size shouldBe 3
        result.array_value?.values?.get(0)?.string_value shouldBe "item1"
        result.array_value?.values?.get(1)?.int_value shouldBe 42
        result.array_value?.values?.get(2)?.bool_value shouldBe true
    }

    @Test
    fun `encodeValue handles Collection with null values by filtering them out`() {
        val collection = listOf("item1", null, "item2")
        val result = encodeValue(collection)

        result.array_value shouldNotBe null
        result.array_value?.values?.size shouldBe 2
        result.array_value?.values?.get(0)?.string_value shouldBe "item1"
        result.array_value?.values?.get(1)?.string_value shouldBe "item2"
    }

    @Test
    fun `encodeValue handles Map values`() {
        val map = mapOf("key1" to "value1", "key2" to 42L)
        val result = encodeValue(map)

        result.kvlist_value shouldNotBe null
        result.kvlist_value?.values?.size shouldBe 2

        val kvPairs = result.kvlist_value!!.values
        kvPairs.size shouldBe 2
        kvPairs[0].key shouldBe "key1"
        kvPairs[1].key shouldBe "key2"
        kvPairs[0].value_?.string_value shouldBe "value1"
        kvPairs[1].value_?.int_value shouldBe 42
    }

    @Test
    fun `encodeValue handles Map with null keys or values by filtering them out`() {
        val map = mapOf("key1" to "value1", null to "value2", "key3" to null)
        val result = encodeValue(map)

        result.kvlist_value shouldNotBe null
        result.kvlist_value?.values?.size shouldBe 1
        result.kvlist_value?.values?.get(0)?.key shouldBe "key1"
        result.kvlist_value?.values?.get(0)?.value_?.string_value shouldBe "value1"
    }

    @Test
    fun `encodeValue throws IllegalArgumentException for unsupported types`() {
        val unsupportedValue = Any()

        shouldThrow<IllegalArgumentException> {
            encodeValue(unsupportedValue) 
        }
    }

    @Test
    fun `SpanData encode converts all fields correctly`() {
        val traceState = TestTraceState(mapOf("key" to "value"))
        val spanContext = TestSpanContext(
            traceId = "1234567890abcdef1234567890abcdef",
            spanId = "1234567890abcdef",
            isRemote = true,
            traceState = traceState
        )
        val attributes = TestAttributes(emptyMap())
        val eventData = TestEventData(
            epochNanos = 1500000L,
            name = "test-event",
            attributes = attributes,
            totalAttributeCount = 0
        )
        val linkSpanContext = TestSpanContext(isRemote = false, traceState = traceState)
        val linkData = TestLinkData(
            spanContext = linkSpanContext,
            attributes = attributes,
            totalAttributeCount = 0
        )
        val resource = Resource.create(attributes)
        val instrLib = TestInstrumentationLibraryInfo(
            "test-lib",
            "1.0.0",
            "http://schema.com"
        )

        val spanData = TestSpanData(
            spanContext = spanContext,
            name = "test-span",
            kind = SpanKind.CLIENT,
            startEpochNanos = 1000000L,
            endEpochNanos = 2000000L,
            attributes = attributes,
            events = listOf(eventData),
            links = listOf(linkData),
            totalAttributeCount = 5,
            totalRecordedEvents = 3,
            totalRecordedLinks = 2,
            resource = resource,
            instrumentationLibraryInfo = instrLib,
            parentSpanContext = TestSpanContext(traceId = "1234567890abcdef1234567890abcdef", spanId = "abcdef1234567890"),
            status = StatusData.create(StatusCode.OK),
            internalHasEnded = true
        )

        val result = spanData.encode()

        assertEquals("1234567890abcdef1234567890abcdef".decodeHex(), result.trace_id)
        assertEquals("1234567890abcdef".decodeHex(), result.span_id)
        assertEquals("key=value", result.trace_state)
        assertEquals("abcdef1234567890".decodeHex(), result.parent_span_id)
        assertEquals("test-span", result.name)
        assertEquals(PbSpan.SpanKind.SPAN_KIND_CLIENT, result.kind)
        assertEquals(1000000L, result.start_time_unix_nano)
        assertEquals(2000000L, result.end_time_unix_nano)
        assertEquals(5, result.dropped_attributes_count) // totalAttributeCount - attributes.size
        assertEquals(1, result.events.size)
        assertEquals(2, result.dropped_events_count) // totalRecordedEvents - events.size
        assertEquals(1, result.links.size)
        assertEquals(1, result.dropped_links_count) // totalRecordedLinks - links.size
    }

    @Test
    fun `SpanData encode handles null parentSpanId`() {
        val traceState = TestTraceState(emptyMap())
        val spanContext = TestSpanContext(isRemote = false, traceState = traceState)
        val attributes = TestAttributes(emptyMap())
        val resource = Resource.create(attributes)
        val instrLib = TestInstrumentationLibraryInfo(
            "test-lib",
            "1.0.0",
            "http://schema.com"
        )

        val spanData = TestSpanData(
            spanContext = spanContext,
            name = "test-span",
            kind = SpanKind.CLIENT,
            startEpochNanos = 1000000L,
            endEpochNanos = 2000000L,
            attributes = attributes,
            events = emptyList(),
            links = emptyList(),
            totalAttributeCount = 5,
            totalRecordedEvents = 3,
            totalRecordedLinks = 2,
            resource = resource,
            instrumentationLibraryInfo = instrLib,
            parentSpanContext = TestSpanContext(),
            status = StatusData.create(StatusCode.OK),
            internalHasEnded = true
        )

        val result = spanData.encode()

        assertEquals(ByteString.EMPTY, result.parent_span_id)
    }

    @Test
    fun `Collection of SpanData encode groups spans by resource and instrumentation library`() {
        val attributes1 = TestAttributes(emptyMap())
        val attributes2 = TestAttributes(emptyMap())
        val resource1 = Resource.create(attributes1, "http://schema1.com")
        val resource2 = Resource.create(attributes2, null)
        val instrLib1 = TestInstrumentationLibraryInfo("lib1", "1.0.0", "http://lib1.com")
        val instrLib2 = TestInstrumentationLibraryInfo("lib2", "2.0.0", null)
        val spanContext = TestSpanContext(
            traceId = "abcdef1234567890abcdef1234567890",
            spanId = "abcdef1234567890",
            isRemote = false
        )

        val spanData1 = TestSpanData(
            spanContext = spanContext,
            name = "span1",
            kind = SpanKind.CLIENT,
            startEpochNanos = 1000000L,
            endEpochNanos = 2000000L,
            attributes = attributes1,
            events = emptyList(),
            links = emptyList(),
            totalAttributeCount = 0,
            totalRecordedEvents = 0,
            totalRecordedLinks = 0,
            parentSpanContext = TestSpanContext(),
            status = StatusData.create(StatusCode.OK),
            instrumentationLibraryInfo = instrLib1,
            resource = resource1,
            internalHasEnded = true,
        )

        val spanData2 = TestSpanData(
            spanContext = spanContext,
            parentSpanContext = TestSpanContext(),
            name = "span2",
            kind = SpanKind.SERVER,
            startEpochNanos = 3000000L,
            endEpochNanos = 4000000L,
            attributes = attributes2,
            events = emptyList(),
            links = emptyList(),
            totalAttributeCount = 0,
            totalRecordedEvents = 0,
            totalRecordedLinks = 0,
            resource = resource2,
            instrumentationLibraryInfo = instrLib2,
            status = StatusData.create(StatusCode.OK),
            internalHasEnded = true
        )

        val spans = listOf(spanData1, spanData2)
        val result = spans.encode()

        result.resource_spans.size shouldBe 2
        result.resource_spans[0].schema_url shouldBe "http://schema1.com"
        result.resource_spans[1].schema_url shouldBe ""
        result.resource_spans[0].scope_spans[0].schema_url shouldBe "http://lib1.com"
        result.resource_spans[1].scope_spans[0].schema_url shouldBe ""
    }

    @Test
    fun `Resource encode converts attributes correctly`() {
        val attributes = TestAttributes(emptyMap())
        val resource = Resource.create(attributes)

        val result = resource.encode()

        result.attributes shouldNotBe null
        result.attributes should { it.isEmpty() }
    }

    @Test
    fun `InstrumentationLibraryInfo encode handles null version`() {
        val instrLib = TestInstrumentationLibraryInfo(
            "test-library",
            null
        )

        val result = instrLib.encode()

        result.name shouldBe "test-library"
        result.version shouldBe ""
    }

    @Test
    fun `InstrumentationLibraryInfo encode handles non-null version`() {
        val instrLib = TestInstrumentationLibraryInfo("test-library", "1.0.0")

        val result = instrLib.encode()

        result.name shouldBe "test-library"
        result.version shouldBe "1.0.0"
    }
}

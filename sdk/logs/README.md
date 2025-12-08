# OpenTelemetry Kotlin Logs SDK

The OpenTelemetry Logs SDK provides a complete implementation of the [OpenTelemetry Logs specification](https://opentelemetry.io/docs/specs/otel/logs/) for Kotlin Multiplatform.

## Table of Contents
- [Quick Start](#quick-start)
- [Basic Usage](#basic-usage)
- [Configuration](#configuration)
- [Log Record Fields](#log-record-fields)
- [Severity Levels](#severity-levels)
- [Attributes](#attributes)
- [Trace Correlation](#trace-correlation)
- [Processors](#processors)
- [Exporters](#exporters)
- [Advanced Usage](#advanced-usage)

## Quick Start

### 1. Add Dependencies

```kotlin
dependencies {
    implementation("io.opentelemetry.kotlin:sdk-logs:$version")
    implementation("io.opentelemetry.kotlin:exporter:$version") // For OTLP exporter
}
```

### 2. Initialize the SDK

```kotlin
import io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider
import io.opentelemetry.kotlin.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.kotlin.exporter.otlp.http.logs.OtlpHttpLogRecordExporter

// Create an OTLP HTTP exporter
val exporter = OtlpHttpLogRecordExporter.builder()
    .setEndpoint("http://localhost:4318")
    .build()

// Create a processor
val processor = SimpleLogRecordProcessor.create(exporter)

// Create the logger provider
val loggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(processor)
    .build()

// Get a logger
val logger = loggerProvider.get("my-application")
```

### 3. Emit Your First Log

```kotlin
logger.logRecordBuilder()
    .setBody("Hello, OpenTelemetry Logs!")
    .setSeverity(Severity.INFO)
    .emit()
```

## Basic Usage

### Creating a Logger

Loggers are obtained from a `LoggerProvider` and are identified by an instrumentation scope name:

```kotlin
// Simple logger with just a name
val logger = loggerProvider.get("my-service")

// Logger with version and schema URL
val logger = loggerProvider.loggerBuilder("my-service")
    .setInstrumentationVersion("1.0.0")
    .setSchemaUrl("https://example.com/my-schema")
    .build()
```

### Emitting Log Records

All log records are created using the builder pattern:

```kotlin
logger.logRecordBuilder()
    .setBody("User logged in successfully")
    .setSeverity(Severity.INFO)
    .setAttribute("user.id", "12345")
    .setAttribute("user.name", "Alice")
    .emit()
```

## Log Record Fields

### Body

The body can be a simple string or a structured value:

```kotlin
// String body
logger.logRecordBuilder()
    .setBody("Simple text message")
    .emit()

// Structured body using Value types
import io.opentelemetry.kotlin.api.common.*

val structuredBody = ValueMap(mapOf(
    "event" to ValueString("user.login"),
    "success" to ValueBoolean(true),
    "timestamp" to ValueLong(System.currentTimeMillis())
))

logger.logRecordBuilder()
    .setBody(structuredBody)
    .emit()
```

### Timestamps

```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit

logger.logRecordBuilder()
    .setBody("Event occurred")
    // Event timestamp (when it actually happened)
    .setTimestamp(1234567890000, DateTimeUnit.MILLISECOND)
    // Observed timestamp (when it was recorded)
    .setObservedTimestamp(Clock.System.now())
    .emit()

// If not set:
// - observedTimestamp defaults to current time
// - timestamp defaults to observedTimestamp
```

## Severity Levels

OpenTelemetry defines 25 severity levels (0-24):

```kotlin
import io.opentelemetry.kotlin.api.log.Severity

// Standard severity levels
logger.logRecordBuilder()
    .setBody("Trace level message")
    .setSeverity(Severity.TRACE)  // Level 1
    .emit()

logger.logRecordBuilder()
    .setBody("Debug information")
    .setSeverity(Severity.DEBUG)  // Level 5
    .emit()

logger.logRecordBuilder()
    .setBody("Informational message")
    .setSeverity(Severity.INFO)   // Level 9
    .emit()

logger.logRecordBuilder()
    .setBody("Warning message")
    .setSeverity(Severity.WARN)   // Level 13
    .emit()

logger.logRecordBuilder()
    .setBody("Error occurred")
    .setSeverity(Severity.ERROR)  // Level 17
    .emit()

logger.logRecordBuilder()
    .setBody("Fatal error")
    .setSeverity(Severity.FATAL)  // Level 21
    .emit()

// Custom severity text
logger.logRecordBuilder()
    .setBody("Custom severity")
    .setSeverity(Severity.INFO)
    .setSeverityText("NOTICE")  // Custom text
    .emit()
```

### Available Severity Levels

| Level | Name | Description |
|-------|------|-------------|
| 0 | UNDEFINED | Unspecified |
| 1-4 | TRACE, TRACE2, TRACE3, TRACE4 | Finest granularity |
| 5-8 | DEBUG, DEBUG2, DEBUG3, DEBUG4 | Debug information |
| 9-12 | INFO, INFO2, INFO3, INFO4 | Informational |
| 13-16 | WARN, WARN2, WARN3, WARN4 | Warning |
| 17-20 | ERROR, ERROR2, ERROR3, ERROR4 | Error |
| 21-24 | FATAL, FATAL2, FATAL3, FATAL4 | Fatal/Critical |

## Attributes

Attributes are key-value pairs that provide additional context:

```kotlin
import io.opentelemetry.kotlin.api.common.AttributeKey

// Using AttributeKey (type-safe)
val userIdKey = AttributeKey.stringKey("user.id")
val requestCountKey = AttributeKey.longKey("request.count")

logger.logRecordBuilder()
    .setBody("User action")
    .setAttribute(userIdKey, "user-123")
    .setAttribute(requestCountKey, 42L)
    .emit()

// Using string keys (convenience methods)
logger.logRecordBuilder()
    .setBody("Request processed")
    .setAttribute("http.method", "POST")
    .setAttribute("http.status_code", 200)
    .setAttribute("response.time_ms", 123.45)
    .setAttribute("success", true)
    .emit()

// Bulk attributes
import io.opentelemetry.kotlin.api.common.Attributes

val attrs = Attributes.builder()
    .put("service.name", "api-gateway")
    .put("service.version", "1.0.0")
    .put("environment", "production")
    .build()

logger.logRecordBuilder()
    .setBody("Service started")
    .setAllAttributes(attrs)
    .emit()
```

## Trace Correlation

Logs are automatically correlated with active traces:

```kotlin
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.api.trace.Span

// When emitting logs inside a span context, they are automatically correlated
val span = tracer.spanBuilder("process-request").startSpan()

span.makeCurrent().use {
    // These logs will include trace_id and span_id
    logger.logRecordBuilder()
        .setBody("Processing request")
        .setSeverity(Severity.INFO)
        .emit()

    // Perform work
    processRequest()

    logger.logRecordBuilder()
        .setBody("Request completed")
        .setSeverity(Severity.INFO)
        .emit()
}

span.end()

// Manual context (advanced)
logger.logRecordBuilder()
    .setBody("Custom context")
    .setContext(Context.current())
    .emit()
```

## Configuration

### Log Record Limits

Control attribute limits to prevent unbounded memory usage:

```kotlin
import io.opentelemetry.kotlin.sdk.logs.LogRecordLimits

val limits = LogRecordLimits.builder()
    .setMaxNumberOfAttributes(128)       // Max 128 attributes per log
    .setMaxAttributeValueLength(1024)    // Truncate strings to 1KB
    .build()

val loggerProvider = SdkLoggerProvider.builder()
    .setLogRecordLimits(limits)
    .addLogRecordProcessor(processor)
    .build()
```

### Resource

Add resource attributes to identify your service:

```kotlin
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.api.common.Attributes

val resource = Resource.create(
    Attributes.builder()
        .put("service.name", "my-service")
        .put("service.version", "1.0.0")
        .put("deployment.environment", "production")
        .put("host.name", "server-01")
        .build()
)

val loggerProvider = SdkLoggerProvider.builder()
    .setResource(resource)
    .addLogRecordProcessor(processor)
    .build()
```

## Processors

### Simple Processor

Exports logs immediately (one at a time):

```kotlin
import io.opentelemetry.kotlin.sdk.logs.export.SimpleLogRecordProcessor

val processor = SimpleLogRecordProcessor.create(exporter)
```

**Use case:** Development, debugging, or when immediate export is required.

### Multiple Processors

Combine multiple processors:

```kotlin
val otlpExporter = OtlpHttpLogRecordExporter.builder()
    .setEndpoint("http://collector:4318")
    .build()

val consoleExporter = ConsoleLogRecordExporter.create()

val loggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(SimpleLogRecordProcessor.create(otlpExporter))
    .addLogRecordProcessor(SimpleLogRecordProcessor.create(consoleExporter))
    .build()
```

## Exporters

### OTLP HTTP Exporter

Export to OpenTelemetry Collector via HTTP:

```kotlin
import io.opentelemetry.kotlin.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import kotlin.time.Duration.Companion.seconds

val exporter = OtlpHttpLogRecordExporter.builder()
    .setEndpoint("http://localhost:4318")
    .setTimeout(10.seconds)
    .setConnectTimeout(5.seconds)
    .addHeader("Authorization", "Bearer token123")
    .build()
```

### Console Exporter

Print logs to console (useful for development):

```kotlin
import io.opentelemetry.kotlin.exporter.ConsoleLogRecordExporter

// Pretty-printed format
val exporter = ConsoleLogRecordExporter.create(prettyPrint = true)

// Raw format
val exporter = ConsoleLogRecordExporter.create(prettyPrint = false)
```

Example output:
```
┌─────────────────────────────────────────────────
│ Log Record
│ Timestamp: 2024-01-15T10:30:45.123Z
│ Observed Timestamp: 2024-01-15T10:30:45.123Z
│ Severity: INFO (9)
│ Body: User logged in successfully
│ Trace ID: 1234567890abcdef1234567890abcdef
│ Span ID: 1234567890abcdef
│ Instrumentation Scope: my-application
│ Attributes:
│   user.id: 12345
│   user.name: Alice
└─────────────────────────────────────────────────
```

### Custom Exporter

Implement your own exporter:

```kotlin
import io.opentelemetry.kotlin.sdk.logs.export.LogRecordExporter
import io.opentelemetry.kotlin.sdk.logs.data.LogRecordData

class MyCustomExporter : LogRecordExporter {
    override suspend fun export(logs: Collection<LogRecordData>) {
        logs.forEach { log ->
            // Send to your backend
            sendToMyBackend(log)
        }
    }

    override suspend fun flush() {
        // Flush any buffered data
    }

    override suspend fun shutdown() {
        // Clean up resources
    }
}
```

## Advanced Usage

### Lifecycle Management

Properly shutdown to ensure all logs are exported:

```kotlin
// On application shutdown
loggerProvider.shutdown()  // Blocks until all logs are exported

// Or with timeout
val result = loggerProvider.shutdown()
result.join(10, DateTimeUnit.SECOND)  // Wait up to 10 seconds

// Force flush without shutdown
loggerProvider.forceFlush()
```

### Using with Closeable

```kotlin
import kotlin.use

SdkLoggerProvider.builder()
    .addLogRecordProcessor(processor)
    .build()
    .use { provider ->
        val logger = provider.get("my-app")

        logger.logRecordBuilder()
            .setBody("Application started")
            .emit()

        // Do work...

        logger.logRecordBuilder()
            .setBody("Application stopping")
            .emit()
    } // Automatically calls shutdown()
```

### Dynamic Configuration

```kotlin
import io.opentelemetry.kotlin.sdk.logs.LogRecordLimits

// Update limits dynamically
val dynamicLimitsSupplier: () -> LogRecordLimits = {
    if (isProductionEnvironment()) {
        LogRecordLimits.builder()
            .setMaxNumberOfAttributes(64)
            .setMaxAttributeValueLength(512)
            .build()
    } else {
        LogRecordLimits.builder()
            .setMaxNumberOfAttributes(256)
            .build()
    }
}

val loggerProvider = SdkLoggerProvider.builder()
    .setLogRecordLimits(dynamicLimitsSupplier)
    .addLogRecordProcessor(processor)
    .build()
```

### Structured Logging with Value Types

```kotlin
import io.opentelemetry.kotlin.api.common.*

// Complex nested structure
val errorDetails = ValueMap(mapOf(
    "error" to ValueMap(mapOf(
        "type" to ValueString("ValidationError"),
        "message" to ValueString("Invalid input"),
        "code" to ValueLong(400),
        "fields" to ValueArray(listOf(
            ValueString("email"),
            ValueString("phone")
        ))
    )),
    "metadata" to ValueMap(mapOf(
        "retry_count" to ValueLong(3),
        "can_retry" to ValueBoolean(false)
    ))
))

logger.logRecordBuilder()
    .setBody(errorDetails)
    .setSeverity(Severity.ERROR)
    .emit()
```

## Complete Example

```kotlin
import io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider
import io.opentelemetry.kotlin.sdk.logs.LogRecordLimits
import io.opentelemetry.kotlin.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.kotlin.exporter.ConsoleLogRecordExporter
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.log.Severity

fun main() {
    // Configure resource
    val resource = Resource.create(
        Attributes.builder()
            .put("service.name", "payment-service")
            .put("service.version", "2.1.0")
            .put("deployment.environment", "production")
            .build()
    )

    // Configure limits
    val limits = LogRecordLimits.builder()
        .setMaxNumberOfAttributes(100)
        .setMaxAttributeValueLength(4096)
        .build()

    // Create exporters
    val otlpExporter = OtlpHttpLogRecordExporter.builder()
        .setEndpoint("http://otel-collector:4318")
        .addHeader("X-API-Key", "secret")
        .build()

    val consoleExporter = ConsoleLogRecordExporter.create()

    // Create logger provider
    val loggerProvider = SdkLoggerProvider.builder()
        .setResource(resource)
        .setLogRecordLimits(limits)
        .addLogRecordProcessor(SimpleLogRecordProcessor.create(otlpExporter))
        .addLogRecordProcessor(SimpleLogRecordProcessor.create(consoleExporter))
        .build()

    // Get logger
    val logger = loggerProvider.loggerBuilder("payment-processor")
        .setInstrumentationVersion("2.1.0")
        .build()

    // Log some events
    logger.logRecordBuilder()
        .setBody("Payment service started")
        .setSeverity(Severity.INFO)
        .setAttribute("startup.duration_ms", 1234)
        .emit()

    try {
        processPayment()

        logger.logRecordBuilder()
            .setBody("Payment processed successfully")
            .setSeverity(Severity.INFO)
            .setAttribute("payment.id", "pay_12345")
            .setAttribute("payment.amount", 99.99)
            .setAttribute("payment.currency", "USD")
            .emit()
    } catch (e: Exception) {
        logger.logRecordBuilder()
            .setBody("Payment processing failed: ${e.message}")
            .setSeverity(Severity.ERROR)
            .setAttribute("error.type", e::class.simpleName ?: "Unknown")
            .setAttribute("error.message", e.message ?: "")
            .emit()
    }

    // Shutdown gracefully
    loggerProvider.shutdown()
}
```

## Best Practices

1. **Reuse LoggerProvider**: Create one provider per application and reuse it
2. **Use Semantic Conventions**: Follow [OpenTelemetry semantic conventions](https://opentelemetry.io/docs/specs/semconv/) for attribute names
3. **Set Appropriate Severity**: Use the correct severity level for your log's importance
4. **Add Context**: Include relevant attributes for filtering and analysis
5. **Graceful Shutdown**: Always call `shutdown()` before application exit
6. **Structured Logging**: Use Value types for complex log bodies instead of string formatting
7. **Trace Correlation**: Emit logs within trace spans for automatic correlation
8. **Limit Attributes**: Configure appropriate limits to prevent memory issues
9. **Resource Attribution**: Always set resource attributes to identify your service

## Performance Considerations

- Logs are emitted asynchronously by default (via `SimpleLogRecordProcessor`)
- Attribute limits prevent unbounded memory growth
- String truncation occurs at the SDK level before export
- Multiple processors execute in parallel
- Use batch processors in production for better throughput (when available)

## Troubleshooting

### Logs not appearing

1. Check that the exporter endpoint is correct
2. Verify the logger provider hasn't been shutdown
3. Ensure `emit()` is being called
4. Check exporter logs for errors

### High memory usage

1. Reduce `maxNumberOfAttributes` in LogRecordLimits
2. Set `maxAttributeValueLength` to truncate long strings
3. Use batch processor instead of simple processor (when available)

### Missing trace correlation

1. Ensure logs are emitted within an active span context
2. Verify the span is made current before emitting logs
3. Check that context propagation is working correctly

## Further Reading

- [OpenTelemetry Logs Specification](https://opentelemetry.io/docs/specs/otel/logs/)
- [Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [OpenTelemetry Collector Documentation](https://opentelemetry.io/docs/collector/)
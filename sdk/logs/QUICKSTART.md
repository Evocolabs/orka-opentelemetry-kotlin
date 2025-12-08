# Quick Start Guide - OpenTelemetry Logs SDK

Get started with OpenTelemetry Logs in 5 minutes.

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.opentelemetry.kotlin:sdk-logs:$version")
    implementation("io.opentelemetry.kotlin:exporter:$version")
}
```

## Minimal Example

```kotlin
import io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider
import io.opentelemetry.kotlin.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.kotlin.exporter.ConsoleLogRecordExporter
import io.opentelemetry.kotlin.api.log.Severity

fun main() {
    // 1. Create a console exporter (prints to stdout)
    val exporter = ConsoleLogRecordExporter.create()

    // 2. Create a processor
    val processor = SimpleLogRecordProcessor.create(exporter)

    // 3. Create logger provider
    val loggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(processor)
        .build()

    // 4. Get a logger
    val logger = loggerProvider.get("my-app")

    // 5. Emit logs!
    logger.logRecordBuilder()
        .setBody("Hello, OpenTelemetry!")
        .setSeverity(Severity.INFO)
        .setAttribute("user", "Alice")
        .emit()

    logger.logRecordBuilder()
        .setBody("Processing request")
        .setSeverity(Severity.DEBUG)
        .setAttribute("request.id", "req-123")
        .setAttribute("duration_ms", 42)
        .emit()

    // 6. Shutdown when done
    loggerProvider.shutdown()
}
```

## With OTLP Collector

Send logs to an OpenTelemetry Collector:

```kotlin
import io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider
import io.opentelemetry.kotlin.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.kotlin.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.kotlin.api.log.Severity

fun main() {
    // Create OTLP exporter
    val exporter = OtlpHttpLogRecordExporter.builder()
        .setEndpoint("http://localhost:4318")  // Default collector endpoint
        .build()

    val processor = SimpleLogRecordProcessor.create(exporter)

    val loggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(processor)
        .build()

    val logger = loggerProvider.get("my-service")

    // Your logs will be sent to the collector
    logger.logRecordBuilder()
        .setBody("Application started")
        .setSeverity(Severity.INFO)
        .emit()

    loggerProvider.shutdown()
}
```

## With Service Resource

Identify your service with resource attributes:

```kotlin
import io.opentelemetry.kotlin.sdk.logs.SdkLoggerProvider
import io.opentelemetry.kotlin.sdk.resources.Resource
import io.opentelemetry.kotlin.api.common.Attributes

val resource = Resource.create(
    Attributes.builder()
        .put("service.name", "my-service")
        .put("service.version", "1.0.0")
        .put("deployment.environment", "production")
        .build()
)

val loggerProvider = SdkLoggerProvider.builder()
    .setResource(resource)
    .addLogRecordProcessor(processor)
    .build()
```

## Common Patterns

### Error Logging

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    logger.logRecordBuilder()
        .setBody("Operation failed: ${e.message}")
        .setSeverity(Severity.ERROR)
        .setAttribute("error.type", e::class.simpleName ?: "Unknown")
        .setAttribute("error.message", e.message ?: "")
        .setAttribute("error.stacktrace", e.stackTraceToString())
        .emit()
}
```

### Debug Logging

```kotlin
logger.logRecordBuilder()
    .setBody("Database query executed")
    .setSeverity(Severity.DEBUG)
    .setAttribute("query.duration_ms", queryTime)
    .setAttribute("query.rows_returned", rowCount)
    .setAttribute("query.sql", sql)
    .emit()
```

### Info Logging

```kotlin
logger.logRecordBuilder()
    .setBody("User action completed")
    .setSeverity(Severity.INFO)
    .setAttribute("user.id", userId)
    .setAttribute("action", "purchase")
    .setAttribute("amount", 99.99)
    .emit()
```

## With Trace Correlation

Logs inside trace spans are automatically correlated:

```kotlin
import io.opentelemetry.kotlin.api.trace.Span

val span = tracer.spanBuilder("process-order").startSpan()

span.makeCurrent().use {
    // This log will include trace_id and span_id
    logger.logRecordBuilder()
        .setBody("Processing order")
        .setSeverity(Severity.INFO)
        .setAttribute("order.id", "order-123")
        .emit()

    processOrder()
}

span.end()
```

## Multiple Exporters

Send logs to multiple destinations:

```kotlin
val otlpExporter = OtlpHttpLogRecordExporter.builder()
    .setEndpoint("http://collector:4318")
    .build()

val consoleExporter = ConsoleLogRecordExporter.create()

val loggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(SimpleLogRecordProcessor.create(otlpExporter))
    .addLogRecordProcessor(SimpleLogRecordProcessor.create(consoleExporter))
    .build()

// Logs will be sent to both collector AND printed to console
```

## Running OpenTelemetry Collector

Use Docker to run a local collector:

```bash
# docker-compose.yml
version: '3'
services:
  otel-collector:
    image: otel/opentelemetry-collector:latest
    ports:
      - "4318:4318"  # OTLP HTTP receiver
      - "13133:13133" # Health check
    volumes:
      - ./otel-collector-config.yaml:/etc/otel/config.yaml
    command: ["--config=/etc/otel/config.yaml"]
```

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:

exporters:
  logging:
    loglevel: debug

  # Or export to observability backend
  # otlp:
  #   endpoint: your-backend:4317

service:
  pipelines:
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging]
```

Start the collector:
```bash
docker-compose up -d
```

Now your logs will be received by the collector and can be forwarded to any backend!

## Next Steps

- Read the [full documentation](README.md) for advanced features
- Check [OpenTelemetry semantic conventions](https://opentelemetry.io/docs/specs/semconv/) for standard attribute names
- Explore [processor options](README.md#processors) for batching and filtering
- Learn about [structured logging](README.md#structured-logging-with-value-types) with Value types

## Troubleshooting

**Logs not showing up?**
- Verify collector is running: `curl http://localhost:4318/v1/logs -X POST`
- Check exporter endpoint matches collector address
- Ensure `emit()` is called on the log record builder
- Call `shutdown()` before app exit to flush remaining logs

**Performance issues?**
- Consider using batch processor when available
- Set attribute limits in `LogRecordLimits`
- Use structured logging instead of string concatenation

**Need help?**
- [OpenTelemetry Kotlin GitHub](https://github.com/open-telemetry/opentelemetry-kotlin)
- [OpenTelemetry Community](https://opentelemetry.io/community/)
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform port of OpenTelemetry-Java, providing tracing and metrics APIs/SDKs for JVM, JS, and native platforms (Windows, macOS, Linux, iOS). The project structure follows a modular architecture with separate API and SDK implementations.

## Build Commands

### Running Tests

Platform-specific test commands (defined in root build.gradle.kts:58-69):

```bash
# macOS tests
./gradlew checkMac

# Linux tests (includes JS and Linux x64)
./gradlew checkLinux

# Windows tests
./gradlew checkWindows
```

Standard Gradle test commands:

```bash
# Run all tests in a specific module
./gradlew :api:all:test

# Run tests for a specific platform
./gradlew :sdk:trace:jvmTest
./gradlew :sdk:trace:jsTest

# Run tests for a single file or test class
./gradlew :sdk:trace:jvmTest --tests "ClassName"
```

### Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :sdk:trace:build

# Build with native compilation (required for full builds)
./gradlew build -PcompileNative=true
```

### Publishing

Platform-specific publish commands (for GitHub Packages):

```bash
./gradlew publishMac
./gradlew publishLinux
./gradlew publishWindows
```

## Module Architecture

The project uses a layered module structure:

### Core Modules

- **`:context`** - Context propagation foundation, provides `ContextKey`, `TextMapPropagator`, and context management primitives
- **`:semconv`** - Semantic conventions for OpenTelemetry attributes

### API Modules (`:api`)

- **`:api:all`** - Unified API module containing tracing, metrics, and baggage APIs
  - Provides `OpenTelemetry`, `Tracer`, `Meter`, `Baggage` interfaces
  - Depends on `:context`
  - Contains no-op implementations (e.g., `DefaultTracer`, `NoopMeterProvider`)

- **`:api:metrics`** - Metrics-specific API definitions

### SDK Modules (`:sdk`)

- **`:sdk:common`** - Shared SDK utilities (`Clock`, `CompletableResultCode`, `InstrumentationLibraryInfo`)
- **`:sdk:trace`** - Tracing SDK implementation
  - Depends on `:api:all`, `:sdk:common`, `:context`
  - Uses kotlinx-collections-immutable and kotlinx-coroutines
- **`:sdk:metrics`** - Metrics SDK implementation
- **`:sdk:testing`** - Testing utilities shared across SDK modules
- **`:sdk:all`** - Aggregated SDK module

### Extension & Export Modules

- **`:extension`** - Extension utilities for trace functionality
- **`:exporter`** - Export implementations including:
  - OTLP HTTP exporter (uses Ktor client + protobuf serialization)
  - Console span exporter
  - Common sender abstractions

## Build System Details

### Custom Gradle Plugins (in `buildSrc/src/main/kotlin/`)

- **`common.gradle.kts`** - Base plugin for Android library configuration (minSdk 21, compileSdk 34)
- **`mpplib.gradle.kts`** - Multiplatform library plugin defining all target platforms:
  - JVM, Android, JS (Node.js + Browser)
  - Native: macOS (arm64, x64), iOS (arm64, x64, simulator), Linux (x64), Windows (mingw x64)
  - Controlled by `compileNative` property (set `-PcompileNative=true`)

### Native Compilation

By default, native targets are NOT compiled. To enable native compilation, set the `compileNative` property:

```bash
./gradlew build -PcompileNative=true
```

In IntelliJ IDEA, only the current platform's native target is compiled for performance.

### Dependencies

Key dependencies (from `gradle/libs.versions.toml`):
- Kotlin 2.1.21
- Kotlinx Coroutines 1.10.2
- Ktor 3.2.3 (HTTP client for OTLP exporter)
- Wire 5.4.0 (Protobuf code generation)
- kotlinx-datetime, kotlinx-collections-immutable
- Kotest assertions for testing

## Code Architecture

### API vs SDK Pattern

The codebase follows OpenTelemetry's API/SDK separation:

- **API modules** provide interfaces and no-op implementations (e.g., `Tracer`, `TracerProvider`)
- **SDK modules** provide full implementations that actually collect and export telemetry data
- Applications depend on APIs, runtime configuration determines SDK usage

### Context Propagation

The `:context` module provides the foundation for propagating telemetry context:
- `ContextKey<T>` - Type-safe keys for storing values in context
- `TextMapPropagator` - Interface for injecting/extracting context from carriers (e.g., HTTP headers)
- Used by both tracing (span context) and baggage

### Tracing Flow

1. Application obtains `Tracer` from `TracerProvider`
2. Creates spans via `Tracer.spanBuilder()`
3. Spans are stored in context via `SpanContextKey`
4. SDK implementation collects span data
5. Exporters (in `:exporter`) send data to backends

### OTLP Exporter Architecture

The `:exporter` module contains the OTLP HTTP exporter:
- Uses **Wire** (Square) for protobuf schema generation from `.proto` files
- Ktor client handles HTTP transport (platform-specific: OkHttp for Android, Darwin for iOS)
- Protobuf encoding via `kotlinx.serialization`
- Sender abstraction in `exporter/sender/` for different transport mechanisms

### Platform-Specific Code

Multiplatform structure:
- `commonMain/` - Shared Kotlin code
- `commonTest/` - Shared tests
- Platform-specific sources when needed (e.g., `androidMain/`, `iosMain/` in exporter for Ktor clients)

## Development Workflow

### CI/CD

GitHub Actions workflow (`.github/workflows/gradle-actions.yml`):
- Runs `checkMac`, `checkLinux`, `checkWindows` on respective runners
- Publishes to GitHub Packages on main branch
- Requires Java 11

### Version Management

Version is determined by:
- `GITHUB_RUN_NUMBER` environment variable in CI (format: `1.0.{RUN_NUMBER}`)
- Defaults to `1.0.0` for local builds
- Can override with `PACKAGE_VERSION` environment variable

### Testing

Most unit tests are ported from OpenTelemetry-Java. Tests use:
- Kotlin test framework (`kotlin.test`)
- Kotest assertions (`kotest-assertions-core`)
- Kotlinx coroutines test utilities

## Common Patterns

### Opt-in Annotations

All modules opt-in to:
- `kotlin.RequiresOptIn` (via compiler args in root build.gradle.kts:25)
- `kotlin.js.ExperimentalJsExport` (in mpplib.gradle.kts:57)

### Atomic Operations

Uses `kotlinx-atomicfu:0.25.0` for lock-free atomic operations across platforms

### Immutable Collections

Heavily uses `kotlinx-collections-immutable` for thread-safe collection handling in SDK

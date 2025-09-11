package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo

class TestInstrumentationLibraryInfo(
    override val name: String,
    override val version: String? = null,
    override val schemaUrl: String? = null
) : InstrumentationLibraryInfo
package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.trace.SpanContext
import io.opentelemetry.kotlin.sdk.trace.data.LinkData

class TestLinkData(
    override val spanContext: SpanContext,
    override val attributes: Attributes,
    override val totalAttributeCount: Int
) : LinkData
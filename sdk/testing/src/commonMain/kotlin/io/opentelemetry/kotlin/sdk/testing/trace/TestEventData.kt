package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.sdk.trace.data.EventData

class TestEventData(
    override val name: String,
    override val attributes: Attributes,
    override val epochNanos: Long,
    override val totalAttributeCount: Int
) : EventData
package io.opentelemetry.kotlin.sdk.testing.trace

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.AttributeType

class TestAttributeKey<T>(
    override val key: String,
    override val type: AttributeType
) : AttributeKey<T>
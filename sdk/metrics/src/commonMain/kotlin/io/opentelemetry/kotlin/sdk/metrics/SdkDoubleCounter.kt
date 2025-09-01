/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.metrics

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.metrics.BoundDoubleCounter
import io.opentelemetry.kotlin.api.metrics.DoubleCounter
import io.opentelemetry.kotlin.api.metrics.DoubleCounterBuilder
import io.opentelemetry.kotlin.api.metrics.LongCounterBuilder
import io.opentelemetry.kotlin.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.metrics.common.InstrumentType
import io.opentelemetry.kotlin.sdk.metrics.common.InstrumentValueType
import io.opentelemetry.kotlin.sdk.metrics.internal.descriptor.InstrumentDescriptor
import io.opentelemetry.kotlin.sdk.metrics.internal.state.BoundStorageHandle
import io.opentelemetry.kotlin.sdk.metrics.internal.state.MeterProviderSharedState
import io.opentelemetry.kotlin.sdk.metrics.internal.state.MeterSharedState
import io.opentelemetry.kotlin.sdk.metrics.internal.state.WriteableMetricStorage

internal class SdkDoubleCounter
private constructor(descriptor: InstrumentDescriptor, storage: WriteableMetricStorage) :
    AbstractInstrument(descriptor), DoubleCounter {
    private val storage: WriteableMetricStorage

    init {
        this.storage = storage
    }

    override fun add(value: Double, attributes: Attributes, context: Context) {
        val aggregatorHandle: BoundStorageHandle = storage.bind(attributes)
        try {
            require(value >= 0) { "Counters can only increase" }
            aggregatorHandle.recordDouble(value, attributes, context)
        } finally {
            aggregatorHandle.release()
        }
    }

    override fun add(value: Double, attributes: Attributes) {
        add(value, attributes, Context.current())
    }

    override fun add(value: Double) {
        add(value, Attributes.empty())
    }

    override fun bind(attributes: Attributes): BoundDoubleCounter {
        return BoundInstrument(storage.bind(attributes), attributes)
    }

    internal class BoundInstrument(handle: BoundStorageHandle, attributes: Attributes) :
        BoundDoubleCounter {
        private val handle: BoundStorageHandle
        private val attributes: Attributes

        init {
            this.handle = handle
            this.attributes = attributes
        }

        override fun add(value: Double, context: Context) {
            require(value >= 0) { "Counters can only increase" }
            handle.recordDouble(value, attributes, context)
        }

        override fun add(value: Double) {
            add(value, Context.current())
        }

        override fun unbind() {
            handle.release()
        }
    }

    internal class Builder(
        meterProviderSharedState: MeterProviderSharedState,
        sharedState: MeterSharedState,
        name: String,
        description: String,
        unit: String
    ) :
        AbstractInstrumentBuilder<Builder>(
            meterProviderSharedState,
            sharedState,
            name,
            description,
            unit
        ),
        DoubleCounterBuilder {

        override val `this`: Builder
            get() = this

        override fun build(): SdkDoubleCounter {
            return buildSynchronousInstrument(InstrumentType.COUNTER, InstrumentValueType.DOUBLE) {
                descriptor: InstrumentDescriptor,
                storage: WriteableMetricStorage ->
                SdkDoubleCounter(descriptor, storage)
            }
        }

        override fun ofLongs(): LongCounterBuilder {
            return swapBuilder(SdkLongCounter::Builder)
        }

        override fun buildWithCallback(callback: (ObservableDoubleMeasurement) -> Unit) {
            registerDoubleAsynchronousInstrument(InstrumentType.OBSERVABLE_SUM, callback)
        }
    }
}

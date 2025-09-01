/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.metrics

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.metrics.BoundLongCounter
import io.opentelemetry.kotlin.api.metrics.DoubleCounterBuilder
import io.opentelemetry.kotlin.api.metrics.LongCounter
import io.opentelemetry.kotlin.api.metrics.LongCounterBuilder
import io.opentelemetry.kotlin.api.metrics.ObservableLongMeasurement
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.sdk.metrics.common.InstrumentType
import io.opentelemetry.kotlin.sdk.metrics.common.InstrumentValueType
import io.opentelemetry.kotlin.sdk.metrics.internal.descriptor.InstrumentDescriptor
import io.opentelemetry.kotlin.sdk.metrics.internal.state.BoundStorageHandle
import io.opentelemetry.kotlin.sdk.metrics.internal.state.MeterProviderSharedState
import io.opentelemetry.kotlin.sdk.metrics.internal.state.MeterSharedState
import io.opentelemetry.kotlin.sdk.metrics.internal.state.WriteableMetricStorage

internal class SdkLongCounter
private constructor(descriptor: InstrumentDescriptor, storage: WriteableMetricStorage) :
    AbstractInstrument(descriptor), LongCounter {
    private val storage: WriteableMetricStorage

    init {
        this.storage = storage
    }

    override fun add(value: Long, attributes: Attributes, context: Context) {
        require(value >= 0) { "Counters can only increase" }
        storage.recordLong(value, attributes, context)
    }

    override fun add(value: Long, attributes: Attributes) {
        add(value, attributes, Context.current())
    }

    override fun add(value: Long) {
        add(value, Attributes.empty())
    }

    override fun bind(attributes: Attributes): BoundLongCounter {
        return BoundInstrument(storage.bind(attributes), attributes)
    }

    internal class BoundInstrument(
        private val handle: BoundStorageHandle,
        private val attributes: Attributes
    ) :
        BoundLongCounter {

        override fun add(value: Long, context: Context) {
            require(value >= 0) { "Counters can only increase" }
            handle.recordLong(value, attributes, context)
        }

        override fun add(value: Long) {
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
        LongCounterBuilder {
        constructor(
            meterProviderSharedState: MeterProviderSharedState,
            meterSharedState: MeterSharedState,
            name: String
        ) : this(meterProviderSharedState, meterSharedState, name, "", "1")

        override val `this`: Builder
            get() = this

        override fun build(): SdkLongCounter {
            return buildSynchronousInstrument(InstrumentType.COUNTER, InstrumentValueType.LONG) {
                descriptor: InstrumentDescriptor,
                storage: WriteableMetricStorage ->
                SdkLongCounter(descriptor, storage)
            }
        }

        override fun ofDoubles(): DoubleCounterBuilder {
            return swapBuilder {
                meterProviderSharedState: MeterProviderSharedState,
                sharedState: MeterSharedState,
                name: String,
                description: String,
                unit: String ->
                SdkDoubleCounter.Builder(
                    meterProviderSharedState,
                    sharedState,
                    name,
                    description,
                    unit
                )
            }
        }

        override fun buildWithCallback(callback: (ObservableLongMeasurement) -> Unit) {
            registerLongAsynchronousInstrument(InstrumentType.OBSERVABLE_SUM, callback)
        }
    }
}

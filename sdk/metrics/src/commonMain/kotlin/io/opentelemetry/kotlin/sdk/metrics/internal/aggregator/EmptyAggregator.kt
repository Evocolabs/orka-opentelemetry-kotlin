/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.metrics.internal.aggregator

import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.kotlin.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.kotlin.sdk.metrics.data.ExemplarData
import io.opentelemetry.kotlin.sdk.metrics.data.MetricData
import io.opentelemetry.kotlin.sdk.metrics.exemplar.ExemplarReservoir
import io.opentelemetry.kotlin.sdk.metrics.internal.descriptor.MetricDescriptor
import io.opentelemetry.kotlin.sdk.resources.Resource

/**
 * A "null object" Aggregator which denotes no aggregation should occur.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class EmptyAggregator private constructor() :
    Aggregator<Unit> {
    override fun createHandle():
        AggregatorHandle<Unit> {
        return HANDLE
    }

    override fun merge(previousCumulative: Unit, delta: Unit): Unit {}

    override fun diff(previousCumulative: Unit, currentCumulative: Unit): Unit {}

    override fun toMetricData(
        resource: Resource,
        instrumentationLibrary: InstrumentationLibraryInfo,
        metricDescriptor: MetricDescriptor,
        accumulationByLabels: Map<Attributes, Unit>,
        temporality: AggregationTemporality,
        startEpochNanos: Long,
        lastCollectionEpoch: Long,
        epochNanos: Long
    ): MetricData {
        throw Exception("Can not create empty metric")
    }

    companion object {
        val INSTANCE: Aggregator<Unit> =
            EmptyAggregator()
        private val HANDLE:
            AggregatorHandle<Unit> =
            object :
                AggregatorHandle<Unit>(
                    ExemplarReservoir.noSamples()
                ) {
                override fun doRecordLong(value: Long) {}
                override fun doRecordDouble(value: Double) {}
                override fun doAccumulateThenReset(exemplars: List<ExemplarData>): Unit {
                    return
                }
            }
    }
}

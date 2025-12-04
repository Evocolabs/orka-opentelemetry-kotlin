/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.api.log.Logger
import io.opentelemetry.kotlin.api.log.LogRecordBuilder
import io.opentelemetry.kotlin.api.log.LoggerProvider
import io.opentelemetry.kotlin.sdk.common.InstrumentationLibraryInfo

/** [SdkLogger] is SDK implementation of [Logger]. */
internal class SdkLogger(
    sharedState: LoggerSharedState,
    instrumentationLibraryInfo: InstrumentationLibraryInfo
) : Logger {
    private val sharedState: LoggerSharedState
    private val instrumentationLibraryInfo: InstrumentationLibraryInfo

    init {
        this.sharedState = sharedState
        this.instrumentationLibraryInfo = instrumentationLibraryInfo
    }

    override fun logRecordBuilder(): LogRecordBuilder {
        if (sharedState.hasBeenShutdown()) {
            val logger = LoggerProvider.noop().get(instrumentationLibraryInfo.name)
            return logger.logRecordBuilder()
        }
        return SdkLogRecordBuilder(
            instrumentationLibraryInfo,
            sharedState,
            sharedState.logRecordLimits
        )
    }

    /**
     * Returns the instrumentation library specified when creating the logger.
     *
     * @return an instance of [InstrumentationLibraryInfo]
     */
    fun getInstrumentationLibraryInfo(): InstrumentationLibraryInfo {
        return instrumentationLibraryInfo
    }
}

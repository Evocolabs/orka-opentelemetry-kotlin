/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.common

import io.opentelemetry.kotlin.api.common.getNanoseconds
import kotlinx.datetime.Clock as DateTimeClock

/** A [Clock] that uses Kotlin Multiplatform time APIs. */
class SystemClock private constructor() : Clock {
    override fun now(): Long {
        // Get current epoch time in nanoseconds using kotlinx-datetime
        return DateTimeClock.System.now().toEpochMilliseconds() * 1_000_000
    }

    override fun nanoTime(): Long {
        // Use TimeSource.Monotonic for high-precision time measurements
        // This provides a monotonic time suitable for measuring durations
        return DateTimeClock.System.now().getNanoseconds()
    }

    companion object {
        private val INSTANCE = SystemClock()

        /**
         * Returns a `MillisClock`.
         *
         * @return a `MillisClock`.
         */
        val instance: Clock
            get() = INSTANCE
    }
}
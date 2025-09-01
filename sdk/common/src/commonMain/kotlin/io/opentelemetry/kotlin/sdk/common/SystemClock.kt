/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.common

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

/** A [Clock] that uses Kotlin Multiplatform time APIs. */
class SystemClock private constructor() : Clock {
    override fun now(): Long {
        // Get current epoch time in nanoseconds using kotlinx-datetime
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds() * 1_000_000L
    }

    override fun nanoTime(): Long {
        // Use TimeSource.Monotonic for high-precision time measurements
        // This provides a monotonic time suitable for measuring durations
        return TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds
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
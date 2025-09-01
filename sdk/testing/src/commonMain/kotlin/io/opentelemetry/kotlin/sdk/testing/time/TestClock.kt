package io.opentelemetry.kotlin.sdk.testing.time

import io.opentelemetry.kotlin.api.common.getNanoseconds
import io.opentelemetry.kotlin.sdk.common.Clock
import kotlin.time.Clock.System.now
import kotlin.time.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class TestClock @OptIn(ExperimentalTime::class)
private constructor(private var current: Instant) : Clock {
    @OptIn(ExperimentalTime::class)
    override fun now(): Long {
        return current.getNanoseconds()
    }

    @OptIn(ExperimentalTime::class)
    override fun nanoTime(): Long {
        return current.getNanoseconds()
    }

    @OptIn(ExperimentalTime::class)
    fun advance(duration: Duration) {
        current = current.plus(duration)
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        fun create(now: Instant): TestClock {
            return TestClock(now)
        }

        @OptIn(ExperimentalTime::class)
        fun create(): TestClock {
            return TestClock(now())
        }
    }
}

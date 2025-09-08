/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.trace

import io.kotest.matchers.shouldBe
import io.opentelemetry.kotlin.sdk.testing.time.TestClock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

/** Unit tests for [AnchoredClock]. */
internal class AnchoredClockTest {
    @OptIn(ExperimentalTime::class)
    @Test
    fun now_PositiveIncrease() {
        val testClock: TestClock = TestClock.create()
        val anchoredClock = AnchoredClock.create(testClock)
        anchoredClock.now() shouldBe testClock.now()
        testClock.advance(3210.nanoseconds)
        anchoredClock.now() shouldBe 1234000008888L
        testClock.advance((-2210).nanoseconds)
        anchoredClock.now() shouldBe 1234000006678L
        testClock.advance(15999993322L.nanoseconds)
        anchoredClock.now() shouldBe 1250000000000L
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun now_NegativeIncrease() {
        val testClock: TestClock = TestClock.create()
        val anchoredClock = AnchoredClock.create(testClock)
        anchoredClock.now() shouldBe testClock.now()
        testClock.advance((-3456).nanoseconds)
        anchoredClock.now() shouldBe 1234000002222L
        testClock.advance(2456.nanoseconds)
        anchoredClock.now() shouldBe 1234000004678L
        testClock.advance((-14000004678L).nanoseconds)
        anchoredClock.now() shouldBe 1220000000000L
    }

    companion object {
        private const val EPOCH_NANOS = 1234000005678L
    }
}

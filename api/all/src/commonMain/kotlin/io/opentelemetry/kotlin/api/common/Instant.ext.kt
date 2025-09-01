package io.opentelemetry.kotlin.api.common

import kotlinx.datetime.DateTimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun Instant.getNanoseconds(): Long {
    return (DateTimeUnit.SECOND.nanoseconds * this.epochSeconds) + this.nanosecondsOfSecond
}

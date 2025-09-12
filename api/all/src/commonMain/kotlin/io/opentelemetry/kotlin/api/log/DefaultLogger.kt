package io.opentelemetry.kotlin.api.log

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.Value
import io.opentelemetry.kotlin.context.Context
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant

internal class DefaultLogger private constructor() : Logger {
    override fun logRecordBuilder(): LogRecordBuilder {
        return NOOP_LOG_RECORD_BUILDER
    }

    private class NoopLogRecordBuilder : LogRecordBuilder {
        override fun setTimestamp(timestamp: Long, unit: DateTimeUnit): LogRecordBuilder {
            return this
        }

        override fun setTimestamp(instant: Instant): LogRecordBuilder {
            return this
        }

        override fun setObservedTimestamp(timestamp: Long, unit: DateTimeUnit): LogRecordBuilder {
            return this
        }

        override fun setObservedTimestamp(instant: Instant): LogRecordBuilder {
            return this
        }

        override fun setContext(context: Context): LogRecordBuilder {
            return this
        }

        override fun setSeverity(severity: Severity): LogRecordBuilder {
            return this
        }

        override fun setSeverityText(severityText: String): LogRecordBuilder {
            return this
        }

        override fun setBody(body: String): LogRecordBuilder {
            return this
        }

        override fun setBody(body: Value<*>): LogRecordBuilder {
            return this
        }

        override fun <T> setAttribute(key: AttributeKey<T>, value: T): LogRecordBuilder {
            return this
        }

        override fun emit() {}
    }

    companion object {
        val instance: Logger = DefaultLogger()
        private val NOOP_LOG_RECORD_BUILDER: LogRecordBuilder = NoopLogRecordBuilder()
    }
}
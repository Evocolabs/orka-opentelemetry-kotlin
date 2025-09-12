package io.opentelemetry.kotlin.api.log

import io.opentelemetry.kotlin.api.common.AttributeKey
import io.opentelemetry.kotlin.api.common.AttributeKey.Companion.booleanKey
import io.opentelemetry.kotlin.api.common.AttributeKey.Companion.doubleKey
import io.opentelemetry.kotlin.api.common.AttributeKey.Companion.longKey
import io.opentelemetry.kotlin.api.common.AttributeKey.Companion.stringKey
import io.opentelemetry.kotlin.api.common.Attributes
import io.opentelemetry.kotlin.api.common.Value
import io.opentelemetry.kotlin.context.Context
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant

/**
 * Used to construct and emit log records from a [Logger].
 *
 *
 * Obtain a [Logger.logRecordBuilder], add properties using the setters, and emit the log
 * record by calling [.emit].
 *
 * @since 1.27.0
 */
interface LogRecordBuilder {
    /**
     * Set the epoch `timestamp`, using the timestamp and unit.
     *
     *
     * The `timestamp` is the time at which the log record occurred. If unset, it will be set
     * to the current time when [.emit] is called.
     */
    fun setTimestamp(timestamp: Long, unit: DateTimeUnit): LogRecordBuilder

    /**
     * Set the epoch `timestamp`, using the instant.
     *
     *
     * The `timestamp` is the time at which the log record occurred. If unset, it will be set
     * to the current time when [.emit] is called.
     */
    fun setTimestamp(instant: Instant): LogRecordBuilder

    /**
     * Set the epoch `observedTimestamp`, using the timestamp and unit.
     *
     *
     * The `observedTimestamp` is the time at which the log record was observed. If unset, it
     * will be set to the `timestamp`. `observedTimestamp` may be different from `timestamp` if logs are being processed asynchronously (e.g. from a file or on a different
     * thread).
     */
    fun setObservedTimestamp(timestamp: Long, unit: DateTimeUnit): LogRecordBuilder

    /**
     * Set the `observedTimestamp`, using the instant.
     *
     *
     * The `observedTimestamp` is the time at which the log record was observed. If unset, it
     * will be set to the `timestamp`. `observedTimestamp` may be different from `timestamp` if logs are being processed asynchronously (e.g. from a file or on a different
     * thread).
     */
    fun setObservedTimestamp(instant: Instant): LogRecordBuilder

    /** Set the context.  */
    fun setContext(context: Context): LogRecordBuilder

    /** Set the severity.  */
    fun setSeverity(severity: Severity): LogRecordBuilder

    /** Set the severity text.  */
    fun setSeverityText(severityText: String): LogRecordBuilder

    /**
     * Set the body string.
     *
     *
     * Shorthand for calling [.setBody] with [Value.of].
     */
    fun setBody(body: String): LogRecordBuilder

    /**
     * Set the body [Value].
     *
     * @since 1.42.0
     */
    fun setBody(body: Value<*>): LogRecordBuilder {
        setBody(body.asString())
        return this
    }

    /**
     * Sets attributes. If the [LogRecordBuilder] previously contained a mapping for any of the
     * keys, the old values are replaced by the specified values.
     */
    @Suppress("UNCHECKED_CAST")
    fun setAllAttributes(attributes: Attributes): LogRecordBuilder {
        if (attributes.isEmpty()) {
            return this
        }
        attributes.forEach { attributeKey, value -> setAttribute(attributeKey as AttributeKey<Any>, value) }
        return this
    }

    /**
     * Sets an attribute on the `LogRecord`. If the `LogRecord` previously contained a
     * mapping for the key, the old value is replaced by the specified value.
     *
     *
     * Note: Providing a null value is a no-op and will not remove previously set values.
     *
     * @param key the key for this attribute.
     * @param value the value for this attribute.
     * @return this.
     */
    fun <T> setAttribute(key: AttributeKey<T>, value: T): LogRecordBuilder

    /**
     * Sets a String attribute on the `LogRecord`. If the `LogRecord` previously contained
     * a mapping for the key, the old value is replaced by the specified value.
     *
     *
     * Note: Providing a null value is a no-op and will not remove previously set values.
     *
     *
     * Note: It is strongly recommended to use [.setAttribute], and
     * pre-allocate your keys, if possible.
     *
     * @param key the key for this attribute.
     * @param value the value for this attribute.
     * @return this.
     * @since 1.48.0
     */
    fun setAttribute(key: String, value: String): LogRecordBuilder {
        return setAttribute(stringKey(key), value)
    }

    /**
     * Sets a Long attribute on the `LogRecord`. If the `LogRecord` previously contained a
     * mapping for the key, the old value is replaced by the specified value.
     *
     *
     * Note: It is strongly recommended to use [.setAttribute], and
     * pre-allocate your keys, if possible.
     *
     * @param key the key for this attribute.
     * @param value the value for this attribute.
     * @return this.
     * @since 1.48.0
     */
    fun setAttribute(key: String, value: Long): LogRecordBuilder {
        return setAttribute(longKey(key), value)
    }

    /**
     * Sets a Double attribute on the `LogRecord`. If the `LogRecord` previously contained
     * a mapping for the key, the old value is replaced by the specified value.
     *
     *
     * Note: It is strongly recommended to use [.setAttribute], and
     * pre-allocate your keys, if possible.
     *
     * @param key the key for this attribute.
     * @param value the value for this attribute.
     * @return this.
     * @since 1.48.0
     */
    fun setAttribute(key: String, value: Double): LogRecordBuilder {
        return setAttribute(doubleKey(key), value)
    }

    /**
     * Sets a Boolean attribute on the `LogRecord`. If the `LogRecord` previously
     * contained a mapping for the key, the old value is replaced by the specified value.
     *
     *
     * Note: It is strongly recommended to use [.setAttribute], and
     * pre-allocate your keys, if possible.
     *
     * @param key the key for this attribute.
     * @param value the value for this attribute.
     * @return this.
     * @since 1.48.0
     */
    fun setAttribute(key: String, value: Boolean): LogRecordBuilder {
        return setAttribute(booleanKey(key), value)
    }

    /**
     * Sets an Integer attribute on the `LogRecord`. If the `LogRecord` previously
     * contained a mapping for the key, the old value is replaced by the specified value.
     *
     *
     * Note: It is strongly recommended to use [.setAttribute], and
     * pre-allocate your keys, if possible.
     *
     * @param key the key for this attribute.
     * @param value the value for this attribute.
     * @return this.
     * @since 1.48.0
     */
    fun setAttribute(key: String, value: Int): LogRecordBuilder {
        return setAttribute(key, value.toLong())
    }

    /**
     * Sets the event name, which identifies the class / type of the Event.
     *
     *
     * This name should uniquely identify the event structure (both attributes and body). A log
     * record with a non-empty event name is an Event.
     *
     * @since 1.50.0
     */
    fun setEventName(eventName: String): LogRecordBuilder {
        return this
    }

    /** Emit the log record.  */
    fun emit()
}
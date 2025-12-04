/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

/**
 * Class that holds limits enforced during log record emission.
 *
 * Note: To allow dynamic updates of [LogRecordLimits] you should register a supplier with
 * [SdkLoggerProviderBuilder.setLogRecordLimits] which supplies dynamic configs when queried.
 */
abstract class LogRecordLimits internal constructor() {
    /**
     * Returns the max number of attributes per [LogRecord].
     *
     * @return the max number of attributes per log record.
     */
    abstract val maxNumberOfAttributes: Int

    /**
     * Returns the max number of characters for string attribute values. For string array
     * attribute values, applies to each entry individually.
     *
     * @return the max number of characters for attribute strings.
     */
    abstract val maxAttributeValueLength: Int

    /**
     * Returns a [LogRecordLimitsBuilder] initialized to the same property values as the current
     * instance.
     *
     * @return a [LogRecordLimitsBuilder] initialized to the same property values as the current
     * instance.
     */
    fun toBuilder(): LogRecordLimitsBuilder {
        return LogRecordLimitsBuilder()
            .setMaxNumberOfAttributes(maxNumberOfAttributes)
            .setMaxAttributeValueLength(maxAttributeValueLength)
    }

    companion object {
        /** Default maximum number of attributes per log record. */
        const val DEFAULT_MAX_NUM_ATTRIBUTES = 128

        /** Default maximum attribute value length (unlimited). */
        const val DEFAULT_MAX_ATTRIBUTE_LENGTH = Int.MAX_VALUE

        /** Returns the default [LogRecordLimits]. */
        val default: LogRecordLimits = LogRecordLimitsBuilder().build()

        /** Returns a new [LogRecordLimitsBuilder] to construct a [LogRecordLimits]. */
        fun builder(): LogRecordLimitsBuilder {
            return LogRecordLimitsBuilder()
        }

        fun create(
            maxNumAttributes: Int,
            maxAttributeLength: Int
        ): LogRecordLimits {
            return Implementation(maxNumAttributes, maxAttributeLength)
        }

        private class Implementation(
            override val maxNumberOfAttributes: Int,
            override val maxAttributeValueLength: Int
        ) : LogRecordLimits()
    }
}

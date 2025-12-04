/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

/** Builder for [LogRecordLimits]. */
class LogRecordLimitsBuilder internal constructor() {
    private var maxNumAttributes = LogRecordLimits.DEFAULT_MAX_NUM_ATTRIBUTES
    private var maxAttributeValueLength: Int = LogRecordLimits.DEFAULT_MAX_ATTRIBUTE_LENGTH

    /**
     * Sets the max number of attributes per log record.
     *
     * @param maxNumberOfAttributes the max number of attributes per log record. Must be positive.
     * @return this.
     * @throws IllegalArgumentException if `maxNumberOfAttributes` is not positive.
     */
    fun setMaxNumberOfAttributes(maxNumberOfAttributes: Int): LogRecordLimitsBuilder {
        require(maxNumberOfAttributes > 0) { "maxNumberOfAttributes must be greater than 0" }
        maxNumAttributes = maxNumberOfAttributes
        return this
    }

    /**
     * Sets the max number of characters for string attribute values. For string array attribute
     * values, applies to each entry individually.
     *
     * @param maxAttributeValueLength the max number of characters for attribute strings. Must not
     * be negative.
     * @return this.
     * @throws IllegalArgumentException if `maxAttributeValueLength` is negative.
     */
    fun setMaxAttributeValueLength(maxAttributeValueLength: Int): LogRecordLimitsBuilder {
        require(maxAttributeValueLength > -1) { "maxAttributeValueLength must be non-negative" }
        this.maxAttributeValueLength = maxAttributeValueLength
        return this
    }

    /**
     * Builds and returns a [LogRecordLimits] with the desired values.
     *
     * @return a [LogRecordLimits] with the desired values.
     */
    fun build(): LogRecordLimits {
        return LogRecordLimits.create(maxNumAttributes, maxAttributeValueLength)
    }
}

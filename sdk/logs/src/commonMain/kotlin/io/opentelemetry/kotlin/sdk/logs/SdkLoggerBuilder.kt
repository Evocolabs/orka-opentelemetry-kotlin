/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.kotlin.sdk.logs

import io.opentelemetry.kotlin.api.log.Logger
import io.opentelemetry.kotlin.api.log.LoggerBuilder
import io.opentelemetry.kotlin.sdk.internal.ComponentRegistry

internal class SdkLoggerBuilder(
    private val registry: ComponentRegistry<SdkLogger>,
    private val instrumentationName: String
) : LoggerBuilder {

    private var instrumentationVersion: String? = null

    private var schemaUrl: String? = null

    override fun setSchemaUrl(schemaUrl: String?): LoggerBuilder {
        this.schemaUrl = schemaUrl
        return this
    }

    override fun setInstrumentationVersion(instrumentationVersion: String?): LoggerBuilder {
        this.instrumentationVersion = instrumentationVersion
        return this
    }

    override fun build(): Logger {
        return registry[instrumentationName, instrumentationVersion, schemaUrl]
    }
}

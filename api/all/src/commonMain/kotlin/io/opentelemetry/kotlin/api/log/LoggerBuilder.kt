package io.opentelemetry.kotlin.api.log

/**
 * Builder class for creating [Logger] instances.
 *
 *
 * [Logger]s are identified by their scope name, version, and schema URL. These identifying
 * fields, along with attributes, combine to form the instrumentation scope, which is attached to
 * all log records produced by the [Logger].
 *
 * @since 1.27.0
 */
interface LoggerBuilder {
    /**
     * Set the scope schema URL of the resulting [Logger]. Schema URL is part of [Logger]
     * identity.
     *
     * @param schemaUrl The schema URL.
     * @return this
     */
    fun setSchemaUrl(schemaUrl: String): LoggerBuilder

    /**
     * Sets the instrumentation scope version of the resulting [Logger]. Version is part of
     * [Logger] identity.
     *
     * @param instrumentationScopeVersion The instrumentation scope version.
     * @return this
     */
    fun setInstrumentationVersion(instrumentationScopeVersion: String): LoggerBuilder

    /**
     * Gets or creates a [Logger] instance.
     *
     * @return a [Logger] instance configured with the provided options.
     */
    fun build(): Logger
}
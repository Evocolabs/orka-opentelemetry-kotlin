package io.opentelemetry.kotlin.api.log

interface LoggerProvider {
    /**
     * Gets or creates a named Logger instance.
     *
     * @param instrumentationScopeName A name uniquely identifying the instrumentation scope, such as
     * the instrumentation library, package, or fully qualified class name. Must not be null.
     * @return a Logger instance.
     */
    fun get(instrumentationScopeName: String): Logger {
        return loggerBuilder(instrumentationScopeName).build()
    }

    /**
     * Creates a LoggerBuilder for a named Logger instance.
     *
     * @param instrumentationScopeName A name uniquely identifying the instrumentation scope, such as
     * the instrumentation library, package, or fully qualified class name. Must not be null.
     * @return a LoggerBuilder instance.
     */
    fun loggerBuilder(instrumentationScopeName: String): LoggerBuilder

    companion object {
        /** Returns a no-op [LoggerProvider] which provides Loggers which do not record or emit.  */
        fun noop(): LoggerProvider = DefaultLoggerProvider.instance
    }
}
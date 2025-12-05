package io.opentelemetry.kotlin.api.log

internal class DefaultLoggerProvider private constructor() : LoggerProvider {
    override fun loggerBuilder(instrumentationScopeName: String): LoggerBuilder {
        return NOOP_BUILDER
    }

    private class NoopLoggerBuilder : LoggerBuilder {
        override fun setSchemaUrl(schemaUrl: String): LoggerBuilder {
            return this
        }

        override fun setInstrumentationVersion(instrumentationScopeVersion: String): LoggerBuilder {
            return this
        }

        override fun build(): Logger {
            return DefaultLogger.instance
        }
    }

    companion object {
        val instance: LoggerProvider = DefaultLoggerProvider()
        private val NOOP_BUILDER: LoggerBuilder = NoopLoggerBuilder()
    }
}
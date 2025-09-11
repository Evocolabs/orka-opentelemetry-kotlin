import io.opentelemetry.kotlin.exporter.common.sender.http.HttpSender

abstract class FailedExportException private constructor(
    cause: Throwable? = null
) : Exception(cause) {

    companion object {
        /** Indicates an HTTP export failed after receiving a response from the server. */
        fun httpFailedWithResponse(response: HttpSender.Response): HttpExportException {
            return HttpExportException(response, null)
        }

        /** Indicates an HTTP export failed exceptionally without receiving a response from the server. */
        fun httpFailedExceptionally(cause: Throwable): HttpExportException {
            return HttpExportException(null, cause)
        }
//
//        /** Indicates a gRPC export failed after receiving a response from the server. */
//        fun grpcFailedWithResponse(response: GrpcResponse): GrpcExportException {
//            return GrpcExportException(response, null)
//        }
//
//        /** Indicates a gRPC export failed exceptionally without receiving a response from the server. */
//        fun grpcFailedExceptionally(cause: Throwable): GrpcExportException {
//            return GrpcExportException(null, cause)
//        }
    }

    /** Returns true if the export failed with a response from the server. */
    abstract fun failedWithResponse(): Boolean

    /**
     * Represents the failure of an HTTP exporter.
     *
     * This class is internal and is hence not for public useAndClose. Its APIs are unstable and can change
     * at any time.
     */
    class HttpExportException internal constructor(
    private val response: HttpSender.Response?,
    private val exceptionCause: Throwable?
    ) : FailedExportException(exceptionCause) {

        override fun failedWithResponse(): Boolean {
            return response != null
        }

        /**
         * Returns the response if the export failed with a response from the server, or null if the
         * export failed exceptionally with no response.
         */
        fun getResponse(): HttpSender.Response? {
        return response
        }

        /**
         * Returns the exceptional cause of failure, or null if the export failed with a response from
         * the server.
         */
        override val cause: Throwable?
            get() = exceptionCause
    }

    /**
     * Represents the failure of a gRPC exporter.
     *
     * This class is internal and is hence not for public useAndClose. Its APIs are unstable and can change
     * at any time.
     */
//    class GrpcExportException internal constructor(
//    private val response: GrpcResponse?,
//    private val exceptionCause: Throwable?
//    ) : FailedExportException(exceptionCause) {
//
//        override fun failedWithResponse(): Boolean {
//            return response != null
//        }
//
//        /**
//         * Returns the response if the export failed with a response from the server, or null if the
//         * export failed exceptionally with no response.
//         */
//        fun getResponse(): GrpcResponse? {
//        return response
//        }
//
//        /**
//         * Returns the exceptional cause of failure, or null if the export failed with a response from
//         * the server.
//         */
//        override val cause: Throwable?
//            get() = exceptionCause
//    }
}
package me.ashishekka.echo.shared.util

/**
 * Interface for platform-agnostic logging.
 */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Global accessor for logging.
 */
object Log {
    private var logger: Logger? = null

    fun init(logger: Logger) {
        this.logger = logger
    }

    fun d(tag: String, message: String) = logger?.d(tag, message)
    fun i(tag: String, message: String) = logger?.i(tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = logger?.e(tag, message, throwable)
}

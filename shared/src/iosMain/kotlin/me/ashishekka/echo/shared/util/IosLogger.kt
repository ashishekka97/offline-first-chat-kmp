package me.ashishekka.echo.shared.util

import platform.Foundation.NSLog

/**
 * iOS implementation of [Logger] using NSLog.
 */
class IosLogger : Logger {
    override fun d(tag: String, message: String) {
        NSLog("DEBUG [$tag]: $message")
    }

    override fun i(tag: String, message: String) {
        NSLog("INFO [$tag]: $message")
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        val errorMessage = if (throwable != null) "$message | Error: ${throwable.message}" else message
        NSLog("ERROR [$tag]: $errorMessage")
    }
}

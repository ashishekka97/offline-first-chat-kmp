package me.ashishekka.echo.shared.domain.util

import kotlin.math.pow
import kotlin.math.roundToInt

object FileSizeUtils {
    /**
     * Formats a file size in bytes into a human-readable string (e.g., "2.3 MB", "45 KB").
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        
        // Round to 1 decimal place if it's MB or larger
        return if (digitGroups > 1) {
            val rounded = (value * 10).roundToInt() / 10.0
            "$rounded ${units[digitGroups]}"
        } else {
            "${value.roundToInt()} ${units[digitGroups]}"
        }
    }
}

package me.ashishekka.echo.shared.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

/**
 * iOS implementation of [StringProvider] using NSLocalizedString.
 */
class IosStringProvider : StringProvider {
    override fun get(key: EchoString): String = when (key) {
        EchoString.JustNow -> "Just now"
        EchoString.MinutesAgo -> "{0}m ago"
        EchoString.Yesterday -> "Yesterday"
        EchoString.Photo -> "Photo"
        EchoString.ImageChat -> "Image Chat"
        EchoString.NewChat -> "New Chat"
        EchoString.MonthJan -> "Jan"
        EchoString.MonthFeb -> "Feb"
        EchoString.MonthMar -> "Mar"
        EchoString.MonthApr -> "Apr"
        EchoString.MonthMay -> "May"
        EchoString.MonthJun -> "Jun"
        EchoString.MonthJul -> "Jul"
        EchoString.MonthAug -> "Aug"
        EchoString.MonthSep -> "Sep"
        EchoString.MonthOct -> "Oct"
        EchoString.MonthNov -> "Nov"
        EchoString.MonthDec -> "Dec"
    }

    override val is24HourFormat: Boolean
        get() {
            val format = NSDateFormatter.dateFormatFromTemplate("j", 0uL, NSLocale.currentLocale)
            return format?.contains("H") == true
        }
}

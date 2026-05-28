package me.ashishekka.echo.shared.util

import android.content.Context

/**
 * Android implementation of [StringProvider] using resources.
 * For the MVP, we can fall back to hardcoded strings if resources aren't set up yet,
 * but this is where they would go.
 */
class AndroidStringProvider(private val context: Context) : StringProvider {
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
}

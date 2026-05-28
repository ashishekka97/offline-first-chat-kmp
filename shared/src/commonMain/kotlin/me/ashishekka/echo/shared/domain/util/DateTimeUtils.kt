package me.ashishekka.echo.shared.domain.util

import kotlinx.datetime.*

object DateTimeUtils {
    /**
     * Formats a timestamp into a "smart" string:
     * - "Just now" (< 1m)
     * - "2m ago" (< 1h)
     * - "2:30 PM" (Today)
     * - "Yesterday" (Yesterday)
     * - "Dec 20" (Within current year)
     * - "20/12/23" (Previous years)
     */
    fun formatSmartTimestamp(
        timestamp: Long,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val messageInstant = Instant.fromEpochMilliseconds(timestamp)
        val diffMs = now.toEpochMilliseconds() - timestamp
        
        if (diffMs < 60_000) return "Just now"
        if (diffMs < 3_600_000) return "${diffMs / 60_000}m ago"
        
        val messageDateTime = messageInstant.toLocalDateTime(timeZone)
        val nowDateTime = now.toLocalDateTime(timeZone)
        
        return if (messageDateTime.date == nowDateTime.date) {
            formatTime(messageDateTime)
        } else if (messageDateTime.date.toEpochDays() == nowDateTime.date.toEpochDays() - 1) {
            "Yesterday"
        } else if (messageDateTime.year == nowDateTime.year) {
            "${getMonthAbbreviation(messageDateTime.month)} ${messageDateTime.dayOfMonth}"
        } else {
            "${messageDateTime.dayOfMonth}/${messageDateTime.monthNumber}/${messageDateTime.year.toString().takeLast(2)}"
        }
    }

    /**
     * Formats a timestamp into a simple time string like "2:30 PM".
     */
    fun formatTime(dateTime: LocalDateTime): String {
        val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
        val minute = dateTime.minute.toString().padStart(2, '0')
        val amPm = if (dateTime.hour < 12) "AM" else "PM"
        return "$hour:$minute $amPm"
    }

    private fun getMonthAbbreviation(month: Month): String = when (month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
        else -> month.name.take(3)
    }
}

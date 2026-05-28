package me.ashishekka.echo.shared.domain.util

import kotlinx.datetime.*
import me.ashishekka.echo.shared.util.EchoString
import me.ashishekka.echo.shared.util.StringProvider

object DateTimeUtils {
    /**
     * Formats a timestamp into a "smart" string using the provided [stringProvider].
     */
    fun formatSmartTimestamp(
        timestamp: Long,
        stringProvider: StringProvider,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val messageInstant = Instant.fromEpochMilliseconds(timestamp)
        val diffMs = now.toEpochMilliseconds() - timestamp
        
        if (diffMs < 60_000) return stringProvider.get(EchoString.JustNow)
        if (diffMs < 3_600_000) {
            val minutes = diffMs / 60_000
            return stringProvider.get(EchoString.MinutesAgo).replace("{0}", minutes.toString())
        }
        
        val messageDateTime = messageInstant.toLocalDateTime(timeZone)
        val nowDateTime = now.toLocalDateTime(timeZone)
        
        return if (messageDateTime.date == nowDateTime.date) {
            formatTime(messageDateTime)
        } else if (messageDateTime.date.toEpochDays() == nowDateTime.date.toEpochDays() - 1) {
            stringProvider.get(EchoString.Yesterday)
        } else if (messageDateTime.year == nowDateTime.year) {
            "${getMonthAbbreviation(messageDateTime.month, stringProvider)} ${messageDateTime.dayOfMonth}"
        } else {
            "${messageDateTime.dayOfMonth}/${messageDateTime.monthNumber}/${messageDateTime.year.toString().takeLast(2)}"
        }
    }

    fun formatTime(dateTime: LocalDateTime): String {
        val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
        val minute = dateTime.minute.toString().padStart(2, '0')
        val amPm = if (dateTime.hour < 12) "AM" else "PM"
        return "$hour:$minute $amPm"
    }

    private fun getMonthAbbreviation(month: Month, stringProvider: StringProvider): String = when (month) {
        Month.JANUARY -> stringProvider.get(EchoString.MonthJan)
        Month.FEBRUARY -> stringProvider.get(EchoString.MonthFeb)
        Month.MARCH -> stringProvider.get(EchoString.MonthMar)
        Month.APRIL -> stringProvider.get(EchoString.MonthApr)
        Month.MAY -> stringProvider.get(EchoString.MonthMay)
        Month.JUNE -> stringProvider.get(EchoString.MonthJun)
        Month.JULY -> stringProvider.get(EchoString.MonthJul)
        Month.AUGUST -> stringProvider.get(EchoString.MonthAug)
        Month.SEPTEMBER -> stringProvider.get(EchoString.MonthSep)
        Month.OCTOBER -> stringProvider.get(EchoString.MonthOct)
        Month.NOVEMBER -> stringProvider.get(EchoString.MonthNov)
        Month.DECEMBER -> stringProvider.get(EchoString.MonthDec)
        else -> month.name.take(3)
    }
}

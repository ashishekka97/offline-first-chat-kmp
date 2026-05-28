package me.ashishekka.echo.shared.util

/**
 * Interface for providing localized strings to shared logic.
 * This prevents hardcoding text like "Just now" in the domain layer.
 */
interface StringProvider {
    fun get(key: EchoString): String
}

/**
 * Keys for all localized strings required by the shared module.
 */
enum class EchoString {
    JustNow,
    MinutesAgo,
    Yesterday,
    Photo,
    ImageChat,
    NewChat,
    MonthJan, MonthFeb, MonthMar, MonthApr, MonthMay, MonthJun,
    MonthJul, MonthAug, MonthSep, MonthOct, MonthNov, MonthDec
}

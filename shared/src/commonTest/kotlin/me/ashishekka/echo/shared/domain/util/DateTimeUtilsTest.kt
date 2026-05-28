package me.ashishekka.echo.shared.domain.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeUtilsTest {

    private val now = Instant.parse("2024-05-28T14:30:00Z")
    private val nowMs = now.toEpochMilliseconds()
    private val timeZone = TimeZone.UTC

    @Test
    fun testJustNow() {
        val timestamp = nowMs - 30_000
        assertEquals("Just now", DateTimeUtils.formatSmartTimestamp(timestamp, now, timeZone))
    }

    @Test
    fun testMinutesAgo() {
        val timestamp = nowMs - 5 * 60_000
        assertEquals("5m ago", DateTimeUtils.formatSmartTimestamp(timestamp, now, timeZone))
    }

    @Test
    fun testTodayTime() {
        val timestamp = nowMs - 3 * 3_600_000
        // 14:30 - 3h = 11:30
        assertEquals("11:30 AM", DateTimeUtils.formatSmartTimestamp(timestamp, now, timeZone))
    }

    @Test
    fun testYesterday() {
        // One day ago
        val timestamp = nowMs - 24 * 3_600_000
        assertEquals("Yesterday", DateTimeUtils.formatSmartTimestamp(timestamp, now, timeZone))
    }

    @Test
    fun testThisYear() {
        val timestamp = Instant.parse("2024-03-15T10:00:00Z").toEpochMilliseconds()
        assertEquals("Mar 15", DateTimeUtils.formatSmartTimestamp(timestamp, now, timeZone))
    }

    @Test
    fun testPreviousYear() {
        val timestamp = Instant.parse("2023-12-20T10:00:00Z").toEpochMilliseconds()
        assertEquals("20/12/23", DateTimeUtils.formatSmartTimestamp(timestamp, now, timeZone))
    }
}

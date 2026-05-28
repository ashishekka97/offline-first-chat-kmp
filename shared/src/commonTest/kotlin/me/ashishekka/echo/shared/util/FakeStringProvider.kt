package me.ashishekka.echo.shared.util

/**
 * Simple implementation of [StringProvider] for unit tests.
 */
class FakeStringProvider : StringProvider {
    override fun get(key: EchoString): String = key.name
    override val is24HourFormat: Boolean = false
}

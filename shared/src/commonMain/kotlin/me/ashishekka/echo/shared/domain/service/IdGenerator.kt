package me.ashishekka.echo.shared.domain.service

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Interface for generating unique identifiers.
 */
interface IdGenerator {
    /**
     * Generates a new unique identifier (UUID string).
     */
    fun generateUuid(): String
}

/**
 * Default implementation of [IdGenerator] using Kotlin's stdlib [Uuid].
 */
@OptIn(ExperimentalUuidApi::class)
class DefaultIdGenerator : IdGenerator {
    override fun generateUuid(): String {
        return Uuid.random().toString()
    }
}

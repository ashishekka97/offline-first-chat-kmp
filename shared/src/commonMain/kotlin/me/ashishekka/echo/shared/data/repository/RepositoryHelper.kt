package me.ashishekka.echo.shared.data.repository

import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result

/**
 * Executes a database [call] and wraps it in a [Result].
 * Maps any [Exception] to [DatabaseError.Unknown].
 */
suspend fun <T> safeDatabaseCall(call: suspend () -> T): Result<T, DatabaseError> {
    return try {
        Result.Success(call())
    } catch (e: Exception) {
        Result.Failure(DatabaseError.Unknown(e))
    }
}

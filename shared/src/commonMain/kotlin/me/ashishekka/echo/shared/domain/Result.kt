package me.ashishekka.echo.shared.domain

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed interface Result<out T, out E : AppError> {
    data class Success<out T>(val data: T) : Result<T, Nothing>
    data class Failure<out E : AppError>(val error: E) : Result<Nothing, E>

    fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
    }

    fun <R, RE : AppError> flatMap(transform: (T) -> Result<R, RE>): Result<R, AppError> = when (this) {
        is Success -> transform(data)
        is Failure -> Failure(error)
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Failure -> error
    }
}

inline fun <T, E : AppError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T, E : AppError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Failure) action(error)
    return this
}

fun <T, E : AppError> Result<T, E>.getOrElse(onFailure: (E) -> T): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> onFailure(error)
}

/**
 * Base interface for all application errors.
 */
interface AppError

/**
 * Common Database errors.
 */
sealed interface DatabaseError : AppError {
    data object NotFound : DatabaseError
    data object ConstraintViolation : DatabaseError
    data class Unknown(val throwable: Throwable) : DatabaseError
}

/**
 * Common IO errors.
 */
sealed interface IOError : AppError {
    data object NotFound : IOError
    data object PermissionDenied : IOError
    data class Unknown(val throwable: Throwable) : IOError
}

/**
 * Asset-specific errors.
 */
sealed interface AssetError : AppError {
    data object NotFound : AssetError
    data object Unreadable : AssetError
    data class WriteFailure(val message: String) : AssetError
    data class Unknown(val throwable: Throwable) : AssetError
}

/**
 * Media processing errors.
 */
sealed interface MediaError : AppError {
    data object InvalidData : MediaError
    data object ProcessingFailed : MediaError
    data class Unknown(val throwable: Throwable) : MediaError
}

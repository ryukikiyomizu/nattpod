package com.skythrew.kattpad.domain

import kotlinx.coroutines.CancellationException

/** Unlike runCatching, never swallow cancellation from a screen leaving composition. */
suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

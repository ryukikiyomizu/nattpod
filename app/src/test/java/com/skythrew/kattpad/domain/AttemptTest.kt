package com.skythrew.kattpad.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AttemptTest {
    @Test fun `network failure becomes a recoverable result`() = runBlocking {
        val result = attempt<Unit> { throw IOException("Offline") }
        assertTrue(result.exceptionOrNull() is IOException)
    }
    @Test(expected = CancellationException::class)
    fun `screen cancellation propagates`() { runBlocking { attempt<Unit> { throw CancellationException("Screen closed") } } }
    @Test fun `successful result is preserved`() = runBlocking { assertEquals(42, attempt { 42 }.getOrThrow()) }
}

package com.skythrew.kattpad.domain

import com.skythrew.kattpad.api.requests.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class ReadingProgressTest {
    private fun story() = StoryData(id = 1, user = MinUserData("writer"))
    private val parts = listOf(StoryPartData(id = 10), StoryPartData(id = 20), StoryPartData(id = 30))
    private fun position(part: Int) = StoryReadingPosition(part, 1, Date(0))

    @Test fun `missing all optional fields is safe`() {
        val progress = story().readingProgress()
        assertNull(progress.partId)
        assertEquals(0, progress.chapter)
        assertEquals(0f, progress.fraction, 0f)
    }
    @Test fun `unstarted story opens its first chapter`() {
        val progress = story().copy(parts = parts).readingProgress()
        assertEquals(10, progress.partId)
        assertEquals(0, progress.chapter)
        assertEquals(3, progress.remaining)
    }
    @Test fun `current chapter is resolved and fraction is bounded`() {
        val progress = story().copy(parts = parts, numParts = 3, readingPosition = position(20)).readingProgress()
        assertEquals(20, progress.partId)
        assertEquals(2, progress.chapter)
        assertEquals(2f / 3f, progress.fraction, 0.001f)
        assertEquals(1, progress.remaining)
    }
    @Test fun `removed chapter falls back instead of opening invalid id`() {
        val progress = story().copy(parts = parts, readingPosition = position(99)).readingProgress()
        assertEquals(10, progress.partId)
        assertEquals(0, progress.chapter)
    }
    @Test fun `missing chapters with a reading position does not invent progress`() {
        val progress = story().copy(numParts = 8, firstPartId = 10, readingPosition = position(20)).readingProgress()
        assertEquals(10, progress.partId)
        assertEquals(0f, progress.fraction, 0f)
    }
    @Test fun `inconsistent chapter totals never exceed one`() {
        val progress = story().copy(numParts = 1, parts = parts, readingPosition = position(30)).readingProgress()
        assertEquals(1f, progress.fraction, 0f)
        assertEquals(0, progress.remaining)
    }
    @Test fun `null chapter ids are ignored`() {
        val progress = story().copy(parts = listOf(StoryPartData(), StoryPartData(id = 10))).readingProgress()
        assertEquals(10, progress.partId)
        assertEquals(1, progress.total)
    }
    @Test fun `negative totals never produce negative remaining or NaN`() {
        val progress = story().copy(numParts = -1).readingProgress()
        assertEquals(0, progress.total)
        assertEquals(0, progress.remaining)
        assertEquals(0f, progress.fraction, 0f)
    }
}

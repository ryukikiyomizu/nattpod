package com.skythrew.kattpad.domain

import com.skythrew.kattpad.api.requests.StoryData

/** Partial API responses and removed chapters must never make a library item unreadable. */
data class ReadingProgress(val partId: Int?, val chapter: Int, val total: Int) {
    val fraction: Float get() = if (total > 0) (chapter.toFloat() / total).coerceIn(0f, 1f) else 0f
    val remaining: Int get() = (total - chapter).coerceAtLeast(0)
}

fun StoryData.readingProgress(): ReadingProgress {
    val chapters = parts.orEmpty().mapNotNull { it.id }
    val total = maxOf(numParts ?: 0, chapters.size)
    val current = readingPosition?.partId
    val index = chapters.indexOf(current)
    return ReadingProgress(
        partId = current?.takeIf { it in chapters } ?: firstPartId ?: chapters.firstOrNull(),
        chapter = if (index >= 0) index + 1 else 0,
        total = total
    )
}

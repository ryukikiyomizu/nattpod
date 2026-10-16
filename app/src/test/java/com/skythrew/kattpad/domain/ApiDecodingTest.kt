package com.skythrew.kattpad.domain

import com.skythrew.kattpad.api.requests.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class ApiDecodingTest {
    private val json = Json { ignoreUnknownKeys = true }
    @Test fun `sparse library story with new server fields stays readable`() {
        val story = json.decodeFromString<StoryData>("""{"id":1,"user":{"name":"writer"},"newField":true}""")
        assertNull(story.readingProgress().partId)
    }
    @Test fun `partial reading position does not reject whole library`() {
        val story = json.decodeFromString<StoryData>("""{"id":1,"user":{"name":"writer"},"readingPosition":{}}""")
        assertEquals(0, story.readingProgress().chapter)
    }
    @Test fun `current user response needs only a username`() {
        val user = json.decodeFromString<UserData>("""{"username":"reader","newField":true}""")
        assertEquals("reader", user.username)
    }
    @Test fun `ISO timestamps accept fractional seconds`() {
        val date = json.decodeFromString(DateSerializer, "\"2026-10-16T12:30:00.123Z\"")
        assertEquals(Instant.parse("2026-10-16T12:30:00.123Z").toEpochMilli(), date.time)
    }
    @Test fun `search dates stay UTC regardless of device timezone`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Manila"))
            val date = json.decodeFromString(SimpleDateSerializer, "\"2026-10-16 12:30:00\"")
            assertEquals(Instant.parse("2026-10-16T12:30:00Z").toEpochMilli(), date.time)
        } finally { TimeZone.setDefault(original) }
    }
}

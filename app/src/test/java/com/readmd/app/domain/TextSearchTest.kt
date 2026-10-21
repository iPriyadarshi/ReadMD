package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {

    @Test
    fun `returns null when there is no match or the query is blank`() {
        assertNull(TextSearch.find("hello world", "xyz"))
        assertNull(TextSearch.find("hello world", "   "))
        assertNull(TextSearch.find("", "a"))
    }

    @Test
    fun `matching is case-insensitive and counts every occurrence`() {
        val match = TextSearch.find("Kotlin and kotlin and KOTLIN", "kotlin")
        assertEquals(3, match?.count)
    }

    @Test
    fun `short text yields the whole text without ellipses`() {
        assertEquals("hello world", TextSearch.find("hello world", "world")?.snippet)
    }

    @Test
    fun `long text is cut around the first match with ellipses`() {
        val text = "a".repeat(100) + " needle " + "b".repeat(100)
        val snippet = TextSearch.find(text, "needle", radius = 10)!!.snippet
        assertTrue(snippet.startsWith("…"))
        assertTrue(snippet.endsWith("…"))
        assertTrue(snippet.contains("needle"))
        assertTrue(snippet.length < 40)
    }

    @Test
    fun `whitespace and newlines collapse to single spaces`() {
        assertEquals("one two three", TextSearch.find("one\n\n  two\tthree", "two")?.snippet)
    }

    @Test
    fun `matches at the very start or end have no leading or trailing ellipsis`() {
        assertEquals("start of text…", TextSearch.find("start of text and more words here", "start", radius = 9)?.snippet)
        assertEquals("…words here end", TextSearch.find("start of text and more words here end", "end", radius = 12)?.snippet)
    }
}

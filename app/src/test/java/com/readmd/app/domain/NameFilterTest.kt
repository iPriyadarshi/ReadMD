package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NameFilterTest {

    private val names = listOf("README.md", "Notes.md", "todo-list.md", "Changelog.markdown")

    @Test
    fun `blank query returns the original list`() {
        assertSame(names, NameFilter.filter(names, "", { it }))
        assertSame(names, NameFilter.filter(names, "   ", { it }))
    }

    @Test
    fun `matches anywhere in the name ignoring case`() {
        assertEquals(listOf("README.md"), NameFilter.filter(names, "readme", { it }))
        assertEquals(listOf("todo-list.md"), NameFilter.filter(names, "LIST", { it }))
        assertEquals(listOf("README.md", "Notes.md", "todo-list.md"), NameFilter.filter(names, ".md", { it }))
    }

    @Test
    fun `surrounding spaces in the query are ignored`() {
        assertEquals(listOf("Notes.md"), NameFilter.filter(names, "  notes ", { it }))
    }

    @Test
    fun `no match returns an empty list`() {
        assertEquals(emptyList<String>(), NameFilter.filter(names, "zzz", { it }))
    }

    @Test
    fun `works on any item type through the name selector`() {
        data class Doc(val id: Int, val name: String)
        val docs = listOf(Doc(1, "Alpha.md"), Doc(2, "Beta.md"))
        assertEquals(listOf(Doc(2, "Beta.md")), NameFilter.filter(docs, "bet", { it.name }))
    }
}

package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTest {

    @Test
    fun `add then parse round-trips id and name`() {
        val entries = Favorites.add(emptySet(), "content://tree/a:b/doc", "Notes.md", now = 5L)
        assertEquals(
            listOf(FavoriteEntry("content://tree/a:b/doc", "Notes.md", 5L)),
            Favorites.parse(entries),
        )
    }

    @Test
    fun `adding the same document again replaces the entry`() {
        val first = Favorites.add(emptySet(), "doc", "Old.md", now = 1L)
        val second = Favorites.add(first, "doc", "New.md", now = 2L)
        assertEquals(listOf(FavoriteEntry("doc", "New.md", 2L)), Favorites.parse(second))
    }

    @Test
    fun `parse lists the newest favorite first`() {
        var entries = emptySet<String>()
        entries = Favorites.add(entries, "a", "A.md", now = 1L)
        entries = Favorites.add(entries, "b", "B.md", now = 3L)
        entries = Favorites.add(entries, "c", "C.md", now = 2L)
        assertEquals(listOf("b", "c", "a"), Favorites.parse(entries).map { it.id })
    }

    @Test
    fun `remove drops only the requested document`() {
        var entries = Favorites.add(emptySet(), "a", "A.md", now = 1L)
        entries = Favorites.add(entries, "b", "B.md", now = 2L)
        assertEquals(listOf("b"), Favorites.parse(Favorites.remove(entries, "a")).map { it.id })
        assertTrue(Favorites.parse(Favorites.remove(emptySet(), "a")).isEmpty())
    }

    @Test
    fun `parse skips malformed entries`() {
        assertTrue(Favorites.parse(setOf("garbage")).isEmpty())
    }
}

package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionsTest {

    @Test
    fun `normalizeName trims collapses whitespace and limits length`() {
        assertEquals("My notes", Collections.normalizeName("  My   notes \n"))
        assertEquals(Collections.MAX_NAME_LENGTH, Collections.normalizeName("x".repeat(100)).length)
        assertEquals("", Collections.normalizeName("   "))
    }

    @Test
    fun `create then parse lists collections sorted by name ignoring case`() {
        var set = emptySet<String>()
        set = Collections.create(set, "1", "work")
        set = Collections.create(set, "2", "Archive")
        assertEquals(listOf("Archive", "work"), Collections.parseCollections(set).map { it.name })
    }

    @Test
    fun `rename changes only the requested collection`() {
        var set = Collections.create(Collections.create(emptySet(), "1", "A"), "2", "B")
        set = Collections.rename(set, "1", "Alpha")
        assertEquals(listOf("Alpha", "B"), Collections.parseCollections(set).map { it.name })
        assertEquals(listOf("1", "2"), Collections.parseCollections(set).map { it.id })
    }

    @Test
    fun `delete removes the collection and its documents`() {
        var set = Collections.create(Collections.create(emptySet(), "1", "A"), "2", "B")
        var items = Collections.assign(emptySet(), "1", "doc1", "One.md")
        items = Collections.assign(items, "2", "doc2", "Two.md")
        set = Collections.delete(set, "1")
        items = Collections.deleteItems(items, "1")
        assertEquals(listOf("2"), Collections.parseCollections(set).map { it.id })
        assertEquals(listOf("doc2"), Collections.parseItems(items).map { it.docId })
    }

    @Test
    fun `assign is idempotent and updates the stored name`() {
        var items = Collections.assign(emptySet(), "1", "doc", "Old.md")
        items = Collections.assign(items, "1", "doc", "New.md")
        val parsed = Collections.parseItems(items)
        assertEquals(1, parsed.size)
        assertEquals("New.md", parsed.single().name)
    }

    @Test
    fun `a document can belong to several collections and be removed from one`() {
        var items = Collections.assign(emptySet(), "1", "doc", "A.md")
        items = Collections.assign(items, "2", "doc", "A.md")
        assertEquals(setOf("1", "2"), Collections.collectionIdsOf(Collections.parseItems(items), "doc"))
        items = Collections.unassign(items, "1", "doc")
        assertEquals(setOf("2"), Collections.collectionIdsOf(Collections.parseItems(items), "doc"))
    }

    @Test
    fun `docsIn lists one collection sorted by name`() {
        var items = Collections.assign(emptySet(), "1", "b", "beta.md")
        items = Collections.assign(items, "1", "a", "Alpha.md")
        items = Collections.assign(items, "2", "c", "Other.md")
        assertEquals(listOf("Alpha.md", "beta.md"), Collections.docsIn(Collections.parseItems(items), "1").map { it.name })
    }

    @Test
    fun `nameTaken ignores case and the collection being renamed`() {
        val list = listOf(DocCollection("1", "Work"), DocCollection("2", "Home"))
        assertTrue(Collections.nameTaken(list, "work"))
        assertFalse(Collections.nameTaken(list, "work", exceptId = "1"))
        assertFalse(Collections.nameTaken(list, "Play"))
    }

    @Test
    fun `doc ids with special characters round-trip`() {
        val id = "content://auth/tree/primary%3ADocs/document/primary%3ADocs%2Fa.md"
        val items = Collections.assign(emptySet(), "1", id, "a.md")
        assertEquals(id, Collections.parseItems(items).single().docId)
    }
}

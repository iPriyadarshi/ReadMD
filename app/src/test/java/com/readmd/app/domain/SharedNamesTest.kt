package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedNamesTest {

    @Test
    fun `shared text is named after its subject or a default`() {
        assertEquals("Meeting notes.md", SharedNames.forText("Meeting notes"))
        assertEquals("Shared text.md", SharedNames.forText(null))
        assertEquals("Shared text.md", SharedNames.forText("   "))
        assertEquals("a b.md", SharedNames.forText("a/b"))
    }

    @Test
    fun `long subjects are shortened`() {
        assertEquals(60 + ".md".length, SharedNames.forText("x".repeat(200)).length)
    }

    @Test
    fun `shared files keep their name and markdown extensions`() {
        assertEquals("notes.md", SharedNames.forFile("notes.md"))
        assertEquals("README.markdown", SharedNames.forFile("README.markdown"))
        assertEquals("todo.txt", SharedNames.forFile("todo.txt"))
    }

    @Test
    fun `files without a markdown or text extension get one`() {
        assertEquals("draft.md", SharedNames.forFile("draft"))
        assertEquals("image.png.md", SharedNames.forFile("image.png"))
    }

    @Test
    fun `unusable file names fall back`() {
        assertEquals("Shared file.md", SharedNames.forFile(null))
        assertEquals("Shared file.md", SharedNames.forFile("..."))
        assertEquals("a b.md", SharedNames.forFile("a:b.md"))
    }

    @Test
    fun `unique names add a counter only when needed`() {
        assertEquals("notes.md", SharedNames.unique("notes.md", emptySet()))
        assertEquals("notes (2).md", SharedNames.unique("notes.md", setOf("notes.md")))
        assertEquals("notes (3).md", SharedNames.unique("notes.md", setOf("notes.md", "notes (2).md")))
    }

    @Test
    fun `unique names work without an extension`() {
        assertEquals("notes (2)", SharedNames.unique("notes", setOf("notes")))
    }
}

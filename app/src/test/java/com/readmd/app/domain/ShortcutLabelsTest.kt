package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutLabelsTest {

    @Test
    fun `labels drop the markdown extension`() {
        assertEquals("Notes", ShortcutLabels.label("Notes.md"))
        assertEquals("Notes", ShortcutLabels.label("Notes.markdown"))
        assertEquals("README", ShortcutLabels.label("README.MD"))
        assertEquals("My file", ShortcutLabels.label("  My file  "))
    }

    @Test
    fun `long names are shortened with an ellipsis`() {
        val label = ShortcutLabels.label("a-very-long-document-name-indeed.md")
        assertEquals(25, label.length)
        assertTrue(label.endsWith("…"))
    }

    @Test
    fun `names that are only an extension or empty still give a label`() {
        assertEquals(".md", ShortcutLabels.label(".md"))
        assertEquals("Document", ShortcutLabels.label("   "))
    }

    @Test
    fun `ids are stable and differ per document`() {
        val a = ShortcutLabels.id("content://auth/tree/x/document/a.md")
        assertEquals(a, ShortcutLabels.id("content://auth/tree/x/document/a.md"))
        assertNotEquals(a, ShortcutLabels.id("content://auth/tree/x/document/b.md"))
        assertTrue(a.startsWith("doc-"))
    }
}

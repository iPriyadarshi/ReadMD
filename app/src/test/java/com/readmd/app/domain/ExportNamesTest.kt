package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportNamesTest {

    @Test
    fun `the markdown extension is replaced by the export extension`() {
        assertEquals("notes.html", ExportNames.fileName("notes.md", "html"))
        assertEquals("notes.html", ExportNames.fileName("notes.markdown", "html"))
        assertEquals("README.pdf", ExportNames.fileName("README.MD", "pdf"))
    }

    @Test
    fun `names without an extension are kept`() {
        assertEquals("My notes.html", ExportNames.fileName("My notes", "html"))
    }

    @Test
    fun `characters file systems reject are replaced and whitespace collapsed`() {
        assertEquals("a b c.html", ExportNames.fileName("a/b:c*.md", "html"))
        assertEquals("one two.html", ExportNames.fileName("  one   two  ", "html"))
    }

    @Test
    fun `empty or dotted names fall back to document`() {
        assertEquals("document.html", ExportNames.fileName("", "html"))
        assertEquals("document.html", ExportNames.fileName("   ", "html"))
        assertEquals("document.html", ExportNames.fileName("...", "html"))
        assertEquals("document.html", ExportNames.fileName(".md", "html"))
    }

    @Test
    fun `very long names are shortened`() {
        val name = ExportNames.fileName("x".repeat(300), "html")
        assertEquals(100 + ".html".length, name.length)
    }
}

package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTablesTest {

    private val wide = "| a | b | c | d | e |\n|---|---|---|---|---|\n| 1 | 2 | 3 | 4 | 5 |"

    @Test
    fun `text without pipes is returned unchanged`() {
        val text = "# Title\n\nNo tables."
        assertSame(text, MarkdownTables.prepare(text).markdown)
    }

    @Test
    fun `narrow tables are left alone`() {
        val md = "| a | b |\n|---|---|\n| 1 | 2 |"
        val prepared = MarkdownTables.prepare(md)
        assertEquals(md, prepared.markdown)
        assertTrue(prepared.tables.isEmpty())
    }

    @Test
    fun `a table with many columns gets a link before it and is captured`() {
        val prepared = MarkdownTables.prepare("Intro\n\n$wide\n\nOutro")
        assertEquals(
            "Intro\n\n[⤢ View wide table](readmd-table:0)\n\n$wide\n\nOutro",
            prepared.markdown,
        )
        val table = prepared.tables.single()
        assertEquals(listOf("a", "b", "c", "d", "e"), table.header)
        assertEquals(listOf(listOf("1", "2", "3", "4", "5")), table.rows)
    }

    @Test
    fun `a table with long cells is wide even with few columns`() {
        val md = "| name | description |\n|---|---|\n| short | ${"x".repeat(60)} |"
        assertEquals(1, MarkdownTables.prepare(md).tables.size)
    }

    @Test
    fun `alignment comes from the delimiter row`() {
        val md = "| a | b | c | d | e |\n|:--|:-:|--:|---|---|\n| 1 | 2 | 3 | 4 | 5 |"
        assertEquals(
            listOf(
                ColumnAlignment.START, ColumnAlignment.CENTER, ColumnAlignment.END,
                ColumnAlignment.START, ColumnAlignment.START,
            ),
            MarkdownTables.prepare(md).tables.single().alignments,
        )
    }

    @Test
    fun `short rows are padded and extra cells are dropped`() {
        val md = "| a | b | c | d | e |\n|---|---|---|---|---|\n| 1 | 2 |\n| 1 | 2 | 3 | 4 | 5 | 6 |"
        val rows = MarkdownTables.prepare(md).tables.single().rows
        assertEquals(listOf("1", "2", "", "", ""), rows[0])
        assertEquals(listOf("1", "2", "3", "4", "5"), rows[1])
    }

    @Test
    fun `tables without outer pipes are recognised`() {
        val md = "a | b | c | d | e\n--|--|--|--|--\n1 | 2 | 3 | 4 | 5"
        assertEquals(5, MarkdownTables.prepare(md).tables.single().header.size)
    }

    @Test
    fun `escaped pipes stay inside their cell`() {
        assertEquals(listOf("a|b", "c"), MarkdownTables.splitRow("| a\\|b | c |"))
    }

    @Test
    fun `inline markdown is stripped from cells`() {
        assertEquals("bold link code", MarkdownTables.plain("**bold** [link](http://x) `code`"))
        assertEquals("alt", MarkdownTables.plain("![alt](img.png)"))
    }

    @Test
    fun `tables inside code fences are ignored`() {
        val md = "```\n$wide\n```"
        val prepared = MarkdownTables.prepare(md)
        assertEquals(md, prepared.markdown)
        assertTrue(prepared.tables.isEmpty())
    }

    @Test
    fun `a pipe line without a delimiter row is not a table`() {
        val md = "a | b | c | d | e\nnot a delimiter\n1 | 2 | 3 | 4 | 5"
        assertTrue(MarkdownTables.prepare(md).tables.isEmpty())
    }

    @Test
    fun `several wide tables are numbered in order`() {
        val prepared = MarkdownTables.prepare("$wide\n\ntext\n\n$wide")
        assertEquals(2, prepared.tables.size)
        assertTrue(prepared.markdown.contains("(readmd-table:0)"))
        assertTrue(prepared.markdown.contains("(readmd-table:1)"))
    }

    @Test
    fun `link indexes are parsed only for table links`() {
        assertEquals(2, MarkdownTables.tableIndex("readmd-table:2"))
        assertNull(MarkdownTables.tableIndex("readmd-mermaid:2"))
        assertFalse(MarkdownTables.isWide(TableData(listOf("a"), listOf(ColumnAlignment.START), emptyList())))
    }
}

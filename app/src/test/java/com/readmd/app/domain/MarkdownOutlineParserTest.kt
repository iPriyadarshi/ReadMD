package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownOutlineParserTest {

    @Test
    fun `extracts atx headings of every level`() {
        val md = """
            # One
            ## Two
            ### Three
            #### Four
            ##### Five
            ###### Six
        """.trimIndent()
        val headings = MarkdownOutlineParser.parse(md)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), headings.map { it.level })
        assertEquals("One", headings[0].text)
        assertEquals("Six", headings[5].text)
    }

    @Test
    fun `ignores headings inside fenced code blocks`() {
        val md = """
            # Real
            ```
            # Not a heading
            ## Also not
            ```
            ## Also real
        """.trimIndent()
        val headings = MarkdownOutlineParser.parse(md)
        assertEquals(listOf("Real", "Also real"), headings.map { it.text })
    }

    @Test
    fun `handles setext headings`() {
        val md = """
            Title
            =====
            Subtitle
            --------
            Body text
        """.trimIndent()
        val headings = MarkdownOutlineParser.parse(md)
        assertEquals(2, headings.size)
        assertEquals(HeadingNode(1, "Title"), headings[0])
        assertEquals(HeadingNode(2, "Subtitle"), headings[1])
    }

    @Test
    fun `strips trailing hashes and inline markers`() {
        val headings = MarkdownOutlineParser.parse("## A **bold** title ##")
        assertEquals(HeadingNode(2, "A bold title"), headings.single())
    }

    @Test
    fun `requires a space after hash`() {
        val headings = MarkdownOutlineParser.parse("#NotAHeading")
        assertTrue(headings.isEmpty())
    }

    @Test
    fun `handles crlf line endings`() {
        val headings = MarkdownOutlineParser.parse("# One\r\n## Two\r\n")
        assertEquals(listOf("One", "Two"), headings.map { it.text })
    }
}

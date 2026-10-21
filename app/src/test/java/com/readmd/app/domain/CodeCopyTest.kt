package com.readmd.app.domain

import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeCopyTest {

    private fun parse(markdown: String): Node = Parser.builder().build().parse(markdown)

    private fun Node.children(): List<Node> = generateSequence(firstChild) { it.next }.toList()

    @Test
    fun `a document without code blocks is untouched`() {
        val doc = parse("# Title\n\nText only.")
        assertTrue(CodeCopy.apply(doc).isEmpty())
        assertEquals(2, doc.children().size)
    }

    @Test
    fun `each fenced block gets a copy link before it and its exact text is returned`() {
        val doc = parse("Intro\n\n```kotlin\nval x = 1\nprintln(x)\n```\n\nOutro")
        val texts = CodeCopy.apply(doc)
        assertEquals(listOf("val x = 1\nprintln(x)"), texts)

        val kinds = doc.children().map { it.javaClass.simpleName }
        assertEquals(listOf("Paragraph", "Paragraph", "FencedCodeBlock", "Paragraph"), kinds)
        val link = (doc.children()[1] as Paragraph).firstChild as Link
        assertEquals("readmd-copy:0", link.destination)
    }

    @Test
    fun `blocks are numbered in document order and keep indentation and blank lines`() {
        val doc = parse("```\nfirst\n```\n\n~~~\n  indented\n\nafter blank\n~~~")
        val texts = CodeCopy.apply(doc)
        assertEquals(listOf("first", "  indented\n\nafter blank"), texts)
        val links = doc.children().filterIsInstance<Paragraph>().map { (it.firstChild as Link).destination }
        assertEquals(listOf("readmd-copy:0", "readmd-copy:1"), links.filter { it.startsWith("readmd-copy:") })
    }

    @Test
    fun `an empty block copies an empty string`() {
        val doc = parse("```\n```")
        assertEquals(listOf(""), CodeCopy.apply(doc))
    }

    @Test
    fun `indented code blocks do not get a copy link`() {
        val doc = parse("Para\n\n    indented code\n")
        assertTrue(CodeCopy.apply(doc).isEmpty())
    }

    @Test
    fun `blocks nested in lists get their link inside the list item`() {
        val doc = parse("- item\n\n  ```\n  code\n  ```\n")
        assertEquals(listOf("code"), CodeCopy.apply(doc))
    }

    @Test
    fun `index parses only copy links`() {
        assertEquals(4, CodeCopy.index("readmd-copy:4"))
        assertNull(CodeCopy.index("readmd-table:4"))
        assertNull(CodeCopy.index("readmd-copy:x"))
    }
}

package com.readmd.app.domain

import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeCopyTest {

    private fun parse(markdown: String): Node = Parser.builder().build().parse(markdown)

    private fun Node.children(): List<Node> = generateSequence(firstChild) { it.next }.toList()

    private fun texts(infos: List<CodeBlockInfo>) = infos.map { it.text }

    private fun linkTargets(paragraph: Node): List<String> =
        paragraph.children().filterIsInstance<Link>().map { it.destination }

    private val longLine = "x".repeat(CodeWrap.LONG_LINE_CHARS + 1)

    @Test
    fun `a document without code blocks is untouched`() {
        val doc = parse("# Title\n\nText only.")
        assertTrue(CodeCopy.apply(doc).isEmpty())
        assertEquals(2, doc.children().size)
    }

    @Test
    fun `each fenced block gets a copy link before it and its exact text is returned`() {
        val doc = parse("Intro\n\n```kotlin\nval x = 1\nprintln(x)\n```\n\nOutro")
        assertEquals(listOf("val x = 1\nprintln(x)"), texts(CodeCopy.apply(doc)))

        val kinds = doc.children().map { it.javaClass.simpleName }
        assertEquals(listOf("Paragraph", "Paragraph", "FencedCodeBlock", "Paragraph"), kinds)
        assertEquals(listOf("readmd-copy:0"), linkTargets(doc.children()[1]))
    }

    @Test
    fun `blocks are numbered in document order and keep indentation and blank lines`() {
        val doc = parse("```\nfirst\n```\n\n~~~\n  indented\n\nafter blank\n~~~")
        assertEquals(listOf("first", "  indented\n\nafter blank"), texts(CodeCopy.apply(doc)))
        val targets = doc.children().filterIsInstance<Paragraph>().flatMap { linkTargets(it) }
        assertEquals(listOf("readmd-copy:0", "readmd-copy:1"), targets)
    }

    @Test
    fun `an empty block copies an empty string`() {
        assertEquals(listOf(""), texts(CodeCopy.apply(parse("```\n```"))))
    }

    @Test
    fun `indented code blocks do not get a copy link`() {
        assertTrue(CodeCopy.apply(parse("Para\n\n    indented code\n")).isEmpty())
    }

    @Test
    fun `blocks nested in lists get their link inside the list item`() {
        assertEquals(listOf("code"), texts(CodeCopy.apply(parse("- item\n\n  ```\n  code\n  ```\n"))))
    }

    @Test
    fun `long lines add a scroll link only when scrolling is chosen`() {
        val markdown = "```\n$longLine\n```"

        val wrap = parse(markdown)
        assertFalse(CodeCopy.apply(wrap, scrollLongLines = false).single().hasScrollLink)
        assertEquals(listOf("readmd-copy:0"), linkTargets(wrap.children()[0]))

        val scroll = parse(markdown)
        assertTrue(CodeCopy.apply(scroll, scrollLongLines = true).single().hasScrollLink)
        assertEquals(listOf("readmd-copy:0", "readmd-codeview:0"), linkTargets(scroll.children()[0]))
    }

    @Test
    fun `short blocks never get a scroll link`() {
        val doc = parse("```\nshort\n```")
        assertFalse(CodeCopy.apply(doc, scrollLongLines = true).single().hasScrollLink)
        assertEquals(listOf("readmd-copy:0"), linkTargets(doc.children()[0]))
    }

    @Test
    fun `link indexes are parsed only for their own links`() {
        assertEquals(4, CodeCopy.index("readmd-copy:4"))
        assertEquals(4, CodeCopy.viewIndex("readmd-codeview:4"))
        assertNull(CodeCopy.index("readmd-codeview:4"))
        assertNull(CodeCopy.viewIndex("readmd-copy:4"))
        assertNull(CodeCopy.index("readmd-copy:x"))
    }
}

package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MermaidBlocksTest {

    @Test
    fun `documents without mermaid are returned unchanged`() {
        val md = "# Title\n\n```kotlin\nval x = 1\n```\n"
        val prepared = MermaidBlocks.prepare(md)
        assertSame(md, prepared.markdown)
        assertTrue(prepared.diagrams.isEmpty())
    }

    @Test
    fun `a mermaid block gets a link before it and its source is captured`() {
        val md = "Intro\n\n```mermaid\ngraph TD\n  A-->B\n```\n\nOutro"
        val prepared = MermaidBlocks.prepare(md)
        assertEquals(listOf("graph TD\n  A-->B"), prepared.diagrams)
        assertEquals(
            "Intro\n\n[▶ View diagram](readmd-mermaid:0)\n\n```mermaid\ngraph TD\n  A-->B\n```\n\nOutro",
            prepared.markdown,
        )
    }

    @Test
    fun `several blocks are numbered in order and other fences are untouched`() {
        val md = "```mermaid\nA\n```\n```js\nconsole.log(1)\n```\n~~~Mermaid\nB\n~~~"
        val prepared = MermaidBlocks.prepare(md)
        assertEquals(listOf("A", "B"), prepared.diagrams)
        assertTrue(prepared.markdown.contains("(readmd-mermaid:0)"))
        assertTrue(prepared.markdown.contains("(readmd-mermaid:1)"))
        assertEquals(2, Regex("View diagram").findAll(prepared.markdown).count())
    }

    @Test
    fun `a mermaid fence inside another fenced block is ignored`() {
        val md = "````markdown\n```mermaid\ngraph TD\n```\n````"
        val prepared = MermaidBlocks.prepare(md)
        assertTrue(prepared.diagrams.isEmpty())
        assertEquals(md, prepared.markdown)
    }

    @Test
    fun `an unclosed mermaid fence runs to the end of the document`() {
        val prepared = MermaidBlocks.prepare("```mermaid\ngraph TD\n  A-->B")
        assertEquals(listOf("graph TD\n  A-->B"), prepared.diagrams)
    }

    @Test
    fun `windows line endings do not leak into the diagram source`() {
        val prepared = MermaidBlocks.prepare("```mermaid\r\nA-->B\r\n```\r\n")
        assertEquals(listOf("A-->B"), prepared.diagrams)
    }

    @Test
    fun `diagramIndex parses only mermaid links`() {
        assertEquals(3, MermaidBlocks.diagramIndex("readmd-mermaid:3"))
        assertNull(MermaidBlocks.diagramIndex("readmd-mermaid:x"))
        assertNull(MermaidBlocks.diagramIndex("https://example.com"))
    }

    @Test
    fun `html escapes the diagram source and picks the theme`() {
        val html = MermaidBlocks.html("A --> B & C <b>", dark = true, background = "#000000", foreground = "#ffffff")
        assertTrue(html.contains("A --&gt; B &amp; C &lt;b&gt;"))
        assertTrue(html.contains("theme: 'dark'"))
        assertTrue(html.contains("#000000"))
        assertFalse(MermaidBlocks.html("A", dark = false, background = "#fff", foreground = "#000").contains("'dark'"))
    }
}

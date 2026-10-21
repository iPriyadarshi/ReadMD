package com.readmd.app.domain

import org.commonmark.node.BlockQuote
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalloutsTest {

    private fun transformed(markdown: String): Node =
        Parser.builder().build().parse(markdown).also { CalloutTransformer.apply(it) }

    private fun Node.children(): List<Node> =
        generateSequence(firstChild) { it.next }.toList()

    private fun textOf(node: Node): String =
        generateSequence(node.firstChild) { it.next }.filterIsInstance<Text>().joinToString("") { it.literal }

    @Test
    fun `marker lines map to their type case-insensitively`() {
        assertEquals(CalloutType.NOTE, CalloutType.fromMarkerLine("[!NOTE]"))
        assertEquals(CalloutType.TIP, CalloutType.fromMarkerLine("[!tip]  "))
        assertEquals(CalloutType.IMPORTANT, CalloutType.fromMarkerLine("[!Important]"))
        assertEquals(CalloutType.WARNING, CalloutType.fromMarkerLine("[!WARNING]"))
        assertEquals(CalloutType.CAUTION, CalloutType.fromMarkerLine("[!CAUTION]"))
        assertNull(CalloutType.fromMarkerLine("[!DANGER]"))
        assertNull(CalloutType.fromMarkerLine("[!NOTE] trailing text"))
        assertNull(CalloutType.fromMarkerLine("plain"))
    }

    @Test
    fun `each type has its own accent and icon in both themes`() {
        assertEquals(CalloutType.entries.size, CalloutType.entries.map { it.lightColor }.toSet().size)
        assertEquals(CalloutType.entries.size, CalloutType.entries.map { it.darkColor }.toSet().size)
        assertEquals(CalloutType.entries.size, CalloutType.entries.map { it.icon }.toSet().size)
        CalloutType.entries.forEach { assertNotEquals(it.lightColor, it.darkColor) }
    }

    @Test
    fun `a callout blockquote becomes a callout block holding the remaining content`() {
        val doc = transformed("> [!NOTE]\n> Useful information.")
        val callout = doc.children().single() as CalloutBlock
        assertEquals(CalloutType.NOTE, callout.type)
        val paragraph = callout.children().single() as Paragraph
        assertEquals("Useful information.", textOf(paragraph))
    }

    @Test
    fun `an ordinary blockquote is left alone`() {
        val doc = transformed("> Just a quote\n> over two lines")
        assertTrue(doc.children().single() is BlockQuote)
    }

    @Test
    fun `a marker with no other content leaves an empty callout`() {
        val doc = transformed("> [!TIP]")
        val callout = doc.children().single() as CalloutBlock
        assertEquals(CalloutType.TIP, callout.type)
        assertNull(callout.firstChild)
    }

    @Test
    fun `further paragraphs stay inside the callout`() {
        val doc = transformed("> [!WARNING]\n> First\n>\n> Second")
        val callout = doc.children().single() as CalloutBlock
        assertEquals(listOf("First", "Second"), callout.children().map { textOf(it) })
    }

    @Test
    fun `a marker that is not on the first line is not a callout`() {
        val doc = transformed("> Intro\n> [!NOTE]\n> text")
        assertTrue(doc.children().single() is BlockQuote)
    }

    @Test
    fun `callouts nested in other quotes are converted too`() {
        val doc = transformed("> Outer\n>\n> > [!CAUTION]\n> > Careful")
        val outer = doc.children().single() as BlockQuote
        assertTrue(outer.children().any { it is CalloutBlock && it.type == CalloutType.CAUTION })
    }
}

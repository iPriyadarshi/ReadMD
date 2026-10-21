package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FootnotesTest {

    @Test
    fun `documents without footnotes are returned unchanged`() {
        val md = "# Title\n\nPlain text [link](x)."
        assertSame(md, Footnotes.prepare(md))
    }

    @Test
    fun `a reference becomes a numbered link and the definition moves to the end`() {
        val md = "Fact[^a] here.\n\n[^a]: The source."
        assertEquals(
            "Fact[¹](readmd-footnote:1) here.\n\n---\n\n**Footnotes**\n\n1. The source. [↩](readmd-footnoteback:1)",
            Footnotes.prepare(md),
        )
    }

    @Test
    fun `footnotes are numbered by first reference and repeated references share a number`() {
        val md = "A[^x] B[^y] C[^x]\n\n[^y]: Why.\n[^x]: Ex."
        val out = Footnotes.prepare(md)
        assertTrue(out.startsWith("A[¹](readmd-footnote:1) B[²](readmd-footnote:2) C[¹](readmd-footnote:1)"))
        assertTrue(out.contains("1. Ex. [↩](readmd-footnoteback:1)\n2. Why. [↩](readmd-footnoteback:2)"))
    }

    @Test
    fun `multi digit numbers use superscript digits`() {
        val refs = (1..10).joinToString(" ") { "t[^n$it]" }
        val defs = (1..10).joinToString("\n") { "[^n$it]: d$it" }
        assertTrue(Footnotes.prepare("$refs\n\n$defs").contains("[¹⁰](readmd-footnote:10)"))
    }

    @Test
    fun `references without a definition are left as text`() {
        val md = "Missing[^nope] and ok[^a]\n\n[^a]: Here."
        val out = Footnotes.prepare(md)
        assertTrue(out.contains("Missing[^nope]"))
        assertTrue(out.contains("ok[¹](readmd-footnote:1)"))
    }

    @Test
    fun `definitions that are never referenced are dropped without a footer`() {
        val md = "No refs here.\n\n[^a]: Orphan."
        assertSame(md, Footnotes.prepare(md))
    }

    @Test
    fun `indented continuation lines extend the definition`() {
        val md = "Text[^a]\n\n[^a]: First part\n    second part"
        assertTrue(Footnotes.prepare(md).contains("1. First part second part [↩]"))
    }

    @Test
    fun `code is never rewritten`() {
        val fenced = "```\nx[^a]\n[^a]: y\n```\nreal[^a]\n\n[^a]: Def."
        val out = Footnotes.prepare(fenced)
        assertTrue(out.startsWith("```\nx[^a]\n[^a]: y\n```\nreal[¹]"))
        val inline = Footnotes.prepare("Use `[^a]` and [^a]\n\n[^a]: Def.")
        assertTrue(inline.startsWith("Use `[^a]` and [¹](readmd-footnote:1)"))
    }

    @Test
    fun `escaped references stay literal`() {
        val out = Footnotes.prepare("\\[^a] and [^a]\n\n[^a]: Def.")
        assertTrue(out.startsWith("\\[^a] and [¹]"))
    }

    @Test
    fun `link numbers are parsed only for footnote links`() {
        assertEquals(3, Footnotes.referenceNumber("readmd-footnote:3"))
        assertEquals(3, Footnotes.backNumber("readmd-footnoteback:3"))
        assertNull(Footnotes.referenceNumber("readmd-footnoteback:3"))
        assertNull(Footnotes.backNumber("https://example.com"))
        assertFalse(Footnotes.prepare("a[^1]\n\n[^1]: b").contains("[^1]"))
    }
}

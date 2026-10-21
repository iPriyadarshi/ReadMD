package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class FrontMatterTest {

    @Test
    fun `documents without front matter are returned unchanged`() {
        val md = "# Title\n\nText"
        val split = FrontMatter.split(md)
        assertNull(split.yaml)
        assertSame(md, split.body)
    }

    @Test
    fun `leading yaml block is separated from the body`() {
        val split = FrontMatter.split("---\ntitle: Hello\ntags: [a, b]\n---\n# Heading\n\nBody")
        assertEquals("title: Hello\ntags: [a, b]", split.yaml)
        assertEquals("# Heading\n\nBody", split.body)
    }

    @Test
    fun `three dots also close the block and a byte order mark is tolerated`() {
        val split = FrontMatter.split("﻿---\ntitle: X\n...\nBody")
        assertEquals("title: X", split.yaml)
        assertEquals("Body", split.body)
    }

    @Test
    fun `windows line endings are handled`() {
        val split = FrontMatter.split("---\r\ntitle: X\r\n---\r\nBody")
        assertEquals("title: X", split.yaml)
        assertEquals("Body", split.body)
    }

    @Test
    fun `an unclosed block is not front matter`() {
        val md = "---\ntitle: X\nno closing fence"
        assertNull(FrontMatter.split(md).yaml)
    }

    @Test
    fun `a horizontal rule followed by prose and another rule is not front matter`() {
        val md = "---\nJust a paragraph between rules\n---\nMore"
        val split = FrontMatter.split(md)
        assertNull(split.yaml)
        assertSame(md, split.body)
    }

    @Test
    fun `entries read plain, quoted and inline list values`() {
        val entries = FrontMatter.entries("title: \"Hello: World\"\nauthor: 'Ann'\ntags: [one, \"two\"]\ndraft: true")
        assertEquals(
            listOf(
                FrontMatterEntry("title", "Hello: World"),
                FrontMatterEntry("author", "Ann"),
                FrontMatterEntry("tags", "one, two"),
                FrontMatterEntry("draft", "true"),
            ),
            entries,
        )
    }

    @Test
    fun `dash lists become comma separated values`() {
        val entries = FrontMatter.entries("tags:\n  - kotlin\n  - android\ntitle: T")
        assertEquals(
            listOf(FrontMatterEntry("tags", "kotlin, android"), FrontMatterEntry("title", "T")),
            entries,
        )
    }

    @Test
    fun `block scalars and wrapped values are joined`() {
        val entries = FrontMatter.entries("description: >\n  first line\n  second line\nnext: ok")
        assertEquals(
            listOf(FrontMatterEntry("description", "first line second line"), FrontMatterEntry("next", "ok")),
            entries,
        )
    }

    @Test
    fun `comments and blank lines are skipped and empty input gives no entries`() {
        assertEquals(listOf(FrontMatterEntry("a", "1")), FrontMatter.entries("# note\n\na: 1"))
        assertEquals(emptyList<FrontMatterEntry>(), FrontMatter.entries(null))
        assertEquals(emptyList<FrontMatterEntry>(), FrontMatter.entries("  "))
    }

    @Test
    fun `the outline of a document body ignores front matter`() {
        val md = "---\ntitle: Hello\n---\n# Real heading"
        val headings = MarkdownOutlineParser.parse(FrontMatter.split(md).body)
        assertEquals(listOf(HeadingNode(1, "Real heading")), headings)
    }
}

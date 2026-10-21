package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class EmojiShortcodesTest {

    private fun p(text: String) = EmojiShortcodes.prepare(text)

    @Test
    fun `text without colons is returned unchanged`() {
        val text = "Plain text"
        assertSame(text, p(text))
    }

    @Test
    fun `common shortcodes become emoji`() {
        assertEquals("Hello 😄 world 🎉", p("Hello :smile: world :tada:"))
        assertEquals("👍 and 👎", p(":+1: and :-1:"))
        assertEquals("💯", p(":100:"))
    }

    @Test
    fun `lookup is case-insensitive and adjacent codes both convert`() {
        assertEquals("😄", p(":SMILE:"))
        assertEquals("😄" + EmojiShortcodes.lookup("heart"), p(":smile::heart:"))
    }

    @Test
    fun `unknown codes are left exactly as written`() {
        assertEquals("keep :not_an_emoji: here", p("keep :not_an_emoji: here"))
        assertEquals("time 10:30:45 today", p("time 10:30:45 today"))
        assertEquals("a : b :: c", p("a : b :: c"))
    }

    @Test
    fun `a colon in front of a known code is not swallowed by an unknown one`() {
        assertEquals("10:30🎉", p("10:30:tada:"))
    }

    @Test
    fun `fenced and inline code are untouched`() {
        val fenced = "```\n:smile:\n```\n:smile:"
        assertEquals("```\n:smile:\n```\n😄", p(fenced))
        assertEquals("Use `:smile:` for 😄", p("Use `:smile:` for :smile:"))
    }

    @Test
    fun `escaped colons stay literal`() {
        assertEquals("\\:smile: 😄", p("\\:smile: :smile:"))
    }

    @Test
    fun `headings convert so the outline shows the emoji`() {
        val headings = MarkdownOutlineParser.parse(p("# Launch :rocket:"))
        assertEquals(listOf(HeadingNode(1, "Launch 🚀")), headings)
    }

    @Test
    fun `lookup exposes known and unknown names`() {
        assertNotNull(EmojiShortcodes.lookup("rocket"))
        assertNull(EmojiShortcodes.lookup("octocat"))
    }
}

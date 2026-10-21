package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeLinkTest {

    private val current = "primary:Docs/guide/intro.md"

    @Test
    fun `urls anchors and absolute paths are not relative file links`() {
        assertFalse(RelativeLink.isRelativeFileLink("https://example.com/a.md"))
        assertFalse(RelativeLink.isRelativeFileLink("mailto:me@example.com"))
        assertFalse(RelativeLink.isRelativeFileLink("content://auth/doc/1"))
        assertFalse(RelativeLink.isRelativeFileLink("#section"))
        assertFalse(RelativeLink.isRelativeFileLink("/root/a.md"))
        assertFalse(RelativeLink.isRelativeFileLink("   "))
    }

    @Test
    fun `plain and dotted paths are relative file links`() {
        assertTrue(RelativeLink.isRelativeFileLink("other.md"))
        assertTrue(RelativeLink.isRelativeFileLink("./other.md"))
        assertTrue(RelativeLink.isRelativeFileLink("../api/index.md#top"))
    }

    @Test
    fun `markdown extensions are recognised case-insensitively`() {
        assertTrue(RelativeLink.isMarkdown("a.MD"))
        assertTrue(RelativeLink.isMarkdown("a.markdown"))
        assertFalse(RelativeLink.isMarkdown("a.png"))
    }

    @Test
    fun `sibling file resolves in the same directory`() {
        assertEquals("primary:Docs/guide/setup.md", RelativeLink.resolve(current, "setup.md"))
        assertEquals("primary:Docs/guide/setup.md", RelativeLink.resolve(current, "./setup.md"))
    }

    @Test
    fun `parent and nested paths resolve`() {
        assertEquals("primary:Docs/api/index.md", RelativeLink.resolve(current, "../api/index.md"))
        assertEquals("primary:Docs/guide/sub/deep.md", RelativeLink.resolve(current, "sub/deep.md"))
    }

    @Test
    fun `fragment and query are dropped and escapes are decoded`() {
        assertEquals("primary:Docs/guide/setup.md", RelativeLink.resolve(current, "setup.md#install"))
        assertEquals("primary:Docs/guide/my notes.md", RelativeLink.resolve(current, "my%20notes.md"))
        assertEquals("primary:Docs/guide/a+b.md", RelativeLink.resolve(current, "a+b.md"))
    }

    @Test
    fun `climbing above the provider root is rejected`() {
        assertNull(RelativeLink.resolve(current, "../../../x.md"))
        assertEquals("primary:x.md", RelativeLink.resolve(current, "../../x.md"))
    }

    @Test
    fun `non hierarchical ids and empty paths cannot be resolved`() {
        assertNull(RelativeLink.resolve("opaque-id-123", "a.md"))
        assertNull(RelativeLink.resolve(current, "#only-anchor"))
        assertNull(RelativeLink.resolve(current, ""))
    }

    @Test
    fun `file at the root resolves siblings at the root`() {
        assertEquals("primary:b.md", RelativeLink.resolve("primary:a.md", "b.md"))
    }
}

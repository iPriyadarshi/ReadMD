package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageNamesTest {

    @Test
    fun `content uris show the decoded file name`() {
        val url = "content://auth/tree/primary%3ADocs/document/primary%3ADocs%2Fimg%2Fa%20b.png"
        assertEquals("a b.png", ImageNames.displayName(url))
    }

    @Test
    fun `relative paths and urls show the last segment`() {
        assertEquals("pic.png", ImageNames.displayName("images/pic.png"))
        assertEquals("pic.png", ImageNames.displayName("https://example.com/a/pic.png?size=2#x"))
    }

    @Test
    fun `a bare name is returned as is`() {
        assertEquals("pic.png", ImageNames.displayName("pic.png"))
    }

    @Test
    fun `a trailing slash is ignored and an empty address falls back to itself`() {
        assertEquals("example.com", ImageNames.displayName("https://example.com/"))
        assertEquals("", ImageNames.displayName(""))
    }
}

package com.readmd.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalLinksTest {

    @Test
    fun `http and https links are web links`() {
        assertTrue(ExternalLinks.isWeb("https://example.com"))
        assertTrue(ExternalLinks.isWeb("http://example.com/a/b?c=d#e"))
        assertTrue(ExternalLinks.isWeb("HTTPS://EXAMPLE.COM"))
        assertTrue(ExternalLinks.isWeb("  https://example.com  "))
    }

    @Test
    fun `other schemes are not web links`() {
        assertFalse(ExternalLinks.isWeb("mailto:me@example.com"))
        assertFalse(ExternalLinks.isWeb("tel:+123456"))
        assertFalse(ExternalLinks.isWeb("content://auth/doc/1"))
        assertFalse(ExternalLinks.isWeb("javascript:alert(1)"))
        assertFalse(ExternalLinks.isWeb("ftp://example.com"))
    }

    @Test
    fun `incomplete or spaced addresses are not web links`() {
        assertFalse(ExternalLinks.isWeb("https://"))
        assertFalse(ExternalLinks.isWeb("example.com"))
        assertFalse(ExternalLinks.isWeb("https://exa mple.com"))
        assertFalse(ExternalLinks.isWeb(""))
    }

    @Test
    fun `fragment only links stay in the page`() {
        assertTrue(ExternalLinks.isInPage("#section"))
        assertFalse(ExternalLinks.isInPage("https://example.com/#section"))
    }
}

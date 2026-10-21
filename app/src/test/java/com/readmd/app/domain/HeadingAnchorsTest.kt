package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingAnchorsTest {

    private fun h(text: String) = HeadingNode(1, text)

    @Test
    fun `plain text is lower-cased and spaces become hyphens`() {
        assertEquals("getting-started", HeadingAnchors.slug("Getting Started"))
    }

    @Test
    fun `punctuation is removed`() {
        assertEquals("hello-world", HeadingAnchors.slug("Hello, World!"))
        assertEquals("whats-new", HeadingAnchors.slug("What's new?"))
    }

    @Test
    fun `removed characters between spaces leave consecutive hyphens like github`() {
        assertEquals("c--kotlin", HeadingAnchors.slug("C++ & Kotlin"))
    }

    @Test
    fun `hyphens underscores digits and non ascii letters are kept`() {
        assertEquals("my_var-2-café", HeadingAnchors.slug("My_Var-2 Café"))
    }

    @Test
    fun `emoji are dropped but the space before them stays`() {
        assertEquals("launch-", HeadingAnchors.slug("Launch 🚀"))
    }

    @Test
    fun `anchors start with a hash`() {
        assertEquals(listOf("#intro", "#usage"), HeadingAnchors.anchors(listOf(h("Intro"), h("Usage"))))
    }

    @Test
    fun `repeated headings are numbered in order`() {
        assertEquals(
            listOf("#intro", "#setup", "#intro-1", "#intro-2"),
            HeadingAnchors.anchors(listOf(h("Intro"), h("Setup"), h("Intro"), h("intro"))),
        )
    }

    @Test
    fun `an empty list gives no anchors`() {
        assertEquals(emptyList<String>(), HeadingAnchors.anchors(emptyList()))
    }
}

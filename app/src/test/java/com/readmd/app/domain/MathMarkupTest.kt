package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MathMarkupTest {

    private fun p(text: String) = MathMarkup.prepare(text)

    @Test
    fun `text without dollars is returned unchanged`() {
        val text = "# Title\n\nNo math here."
        assertSame(text, p(text))
    }

    @Test
    fun `single dollar inline math becomes double dollar`() {
        assertEquals("Energy ${'$'}${'$'}E=mc^2${'$'}${'$'} is famous", p("Energy ${'$'}E=mc^2${'$'} is famous"))
    }

    @Test
    fun `several inline formulas on one line are all converted`() {
        assertEquals(
            "${'$'}${'$'}a${'$'}${'$'} and ${'$'}${'$'}b+1${'$'}${'$'}",
            p("${'$'}a${'$'} and ${'$'}b+1${'$'}"),
        )
    }

    @Test
    fun `prices are not treated as math`() {
        assertEquals("It costs ${'$'}5 and ${'$'}10 total", p("It costs ${'$'}5 and ${'$'}10 total"))
        assertEquals("From ${'$'}5.00-${'$'}6.00 only", p("From ${'$'}5.00-${'$'}6.00 only"))
        assertEquals("Lone ${'$'} sign", p("Lone ${'$'} sign"))
    }

    @Test
    fun `dollar followed by a space does not open math`() {
        assertEquals("a ${'$'} b ${'$'} c", p("a ${'$'} b ${'$'} c"))
    }

    @Test
    fun `existing double dollar math and block delimiters are untouched`() {
        assertEquals("Inline ${'$'}${'$'}x${'$'}${'$'} here", p("Inline ${'$'}${'$'}x${'$'}${'$'} here"))
        val block = "${'$'}${'$'}\n\\int_0^1 x\\,dx = ${'$'}1/2${'$'}\n${'$'}${'$'}"
        assertEquals(block, p(block))
    }

    @Test
    fun `fenced code and inline code are never rewritten`() {
        val fenced = "```\nprice = ${'$'}x + ${'$'}y\n```"
        assertEquals(fenced, p(fenced))
        assertEquals("Use `${'$'}x${'$'}` for math", p("Use `${'$'}x${'$'}` for math"))
    }

    @Test
    fun `math after a closed code fence is converted again`() {
        assertEquals(
            "```\ncode\n```\n${'$'}${'$'}y${'$'}${'$'}",
            p("```\ncode\n```\n${'$'}y${'$'}"),
        )
    }

    @Test
    fun `escaped dollars are left alone`() {
        assertEquals("Pay \\${'$'}x\\${'$'} now", p("Pay \\${'$'}x\\${'$'} now"))
    }

    @Test
    fun `an unmatched opening dollar stays as text`() {
        assertEquals("Only ${'$'}x open", p("Only ${'$'}x open"))
    }
}

package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlExportTest {

    private fun render(markdown: String, imageSource: (String) -> String? = { null }) =
        HtmlExport.render("Doc <1>", markdown, imageSource)

    @Test
    fun `a complete page is produced with an escaped title`() {
        val html = render("Hello")
        assertTrue(html.startsWith("<!doctype html>"))
        assertTrue(html.contains("<title>Doc &lt;1&gt;</title>"))
        assertTrue(html.contains("<p>Hello</p>"))
    }

    @Test
    fun `headings emphasis lists and code are rendered`() {
        val html = render("# Title\n\n**bold** and *it*\n\n- a\n- b\n\n```kotlin\nval x = 1\n```")
        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<li>a</li>"))
        assertTrue(html.contains("<pre><code class=\"language-kotlin\">val x = 1\n</code></pre>"))
    }

    @Test
    fun `tables and strikethrough use the github extensions`() {
        val html = render("| a | b |\n|---|---|\n| 1 | 2 |\n\n~~gone~~")
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>a</th>"))
        assertTrue(html.contains("<td>2</td>"))
        assertTrue(html.contains("<del>gone</del>"))
    }

    @Test
    fun `raw html in the markdown is shown as text`() {
        val html = render("<script>alert(1)</script>\n\n<b>x</b>")
        assertFalse(html.contains("<script>alert"))
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
    }

    @Test
    fun `script links are neutralised`() {
        val html = render("[click](javascript:alert(1)) and [ok](https://example.com)")
        assertFalse(html.contains("javascript:"))
        assertTrue(html.contains("<a href=\"#\">click</a>"))
        assertTrue(html.contains("<a href=\"https://example.com\">ok</a>"))
    }

    @Test
    fun `front matter becomes a metadata table and is not part of the body`() {
        val html = render("---\ntitle: Hello\ntags: [a, b]\n---\n# Heading")
        assertTrue(html.contains("<table class=\"meta\">"))
        assertTrue(html.contains("<th>title</th><td>Hello</td>"))
        assertTrue(html.contains("<th>tags</th><td>a, b</td>"))
        assertTrue(html.contains("<h1>Heading</h1>"))
        assertFalse(html.contains("<hr"))
    }

    @Test
    fun `emoji shortcodes are replaced`() {
        assertTrue(render("Done :tada:").contains("Done 🎉"))
    }

    @Test
    fun `footnote markers lose their links and the list is kept without back arrows`() {
        val html = render("Fact[^a].\n\n[^a]: The source.")
        assertTrue(html.contains("Fact¹."))
        assertTrue(html.contains("The source."))
        assertFalse(html.contains("readmd-footnote"))
        assertFalse(html.contains("↩"))
    }

    @Test
    fun `task list markers become check boxes`() {
        val html = render("- [ ] todo\n- [x] done")
        assertTrue(html.contains("☐ todo"))
        assertTrue(html.contains("☑ done"))
    }

    @Test
    fun `callouts are drawn as titled boxes and plain quotes are unchanged`() {
        val html = render("> [!WARNING]\n> Careful here\n\n> just a quote")
        assertTrue(html.contains("<div class=\"callout callout-warning\">"))
        assertTrue(html.contains("<p class=\"callout-title\">⚠️ Warning</p>"))
        assertTrue(html.contains("Careful here"))
        assertTrue(html.contains("<blockquote>"))
    }

    @Test
    fun `image addresses can be swapped for embedded data`() {
        val html = render("![logo](images/a.png) ![web](https://x.test/b.png)") { source ->
            if (source == "images/a.png") "data:image/png;base64,AAAA" else null
        }
        assertTrue(html.contains("src=\"data:image/png;base64,AAAA\""))
        assertTrue(html.contains("src=\"https://x.test/b.png\""))
    }

    @Test
    fun `escape handles the characters that matter in attributes and text`() {
        assertEquals("&lt;a href=&quot;x&quot;&gt; &amp;", HtmlExport.escape("<a href=\"x\"> &"))
    }
}

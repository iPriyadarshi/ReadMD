package com.readmd.app.domain

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Turns a markdown document into one self-contained HTML page, used for printing and for
 * exporting to PDF or HTML. It follows the reader where it can: front matter becomes a
 * metadata table, emoji shortcodes are replaced, footnotes are collected at the end, and
 * callouts, tables, code and task lists are styled. Raw HTML in the markdown is shown as
 * text and script links are neutralised. Math and diagrams are not drawn; they appear as
 * their source.
 */
object HtmlExport {

    private val EXTENSIONS: List<Extension> = listOf(TablesExtension.create(), StrikethroughExtension.create())

    private val parser: Parser = Parser.builder().extensions(EXTENSIONS).build()

    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .extensions(EXTENSIONS)
        .escapeHtml(true)
        .nodeRendererFactory { context -> CalloutHtmlRenderer(context) }
        .build()

    private val FOOTNOTE_LINK = Regex("<a href=\"readmd-footnote:\\d+\">(.*?)</a>")
    private val FOOTNOTE_BACK = Regex("\\s*<a href=\"readmd-footnoteback:\\d+\">.*?</a>")
    private val UNSAFE_URL = Regex("(href|src)=\"\\s*(?:javascript|vbscript):[^\"]*\"", RegexOption.IGNORE_CASE)
    private val IMAGE_SRC = Regex("(<img\\b[^>]*?\\ssrc=\")([^\"]*)(\")")
    private val TASK_OPEN = Regex("<li>(<p>)?\\[ ] ")
    private val TASK_DONE = Regex("<li>(<p>)?\\[[xX]] ")

    /**
     * @param imageSource maps an image address from the markdown to what the page should
     * load (for example an embedded data URI); return null to keep the address as written.
     */
    fun render(
        title: String,
        markdown: String,
        imageSource: (String) -> String? = { null },
    ): String {
        val split = FrontMatter.split(markdown)
        val body = Footnotes.prepare(EmojiShortcodes.prepare(split.body))
        val document: Node = parser.parse(body)
        CalloutTransformer.apply(document)

        var html = renderer.render(document)
        html = FOOTNOTE_LINK.replace(html) { it.groupValues[1] }
        html = FOOTNOTE_BACK.replace(html, "")
        html = UNSAFE_URL.replace(html) { "${it.groupValues[1]}=\"#\"" }
        html = TASK_OPEN.replace(html) { "<li>${it.groupValues[1]}☐ " }
        html = TASK_DONE.replace(html) { "<li>${it.groupValues[1]}☑ " }
        html = IMAGE_SRC.replace(html) { match ->
            val source = match.groupValues[2]
            match.groupValues[1] + (imageSource(unescapeAttribute(source)) ?: source) + match.groupValues[3]
        }

        return buildString {
            append("<!doctype html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            append("<title>").append(escape(title)).append("</title>\n<style>\n").append(STYLE).append("</style>\n")
            append("</head>\n<body>\n")
            appendMetadata(FrontMatter.entries(split.yaml))
            append(html)
            append("</body>\n</html>\n")
        }
    }

    private fun StringBuilder.appendMetadata(entries: List<FrontMatterEntry>) {
        if (entries.isEmpty()) return
        append("<table class=\"meta\">\n")
        for (entry in entries) {
            append("<tr><th>").append(escape(entry.key)).append("</th><td>")
                .append(escape(entry.value)).append("</td></tr>\n")
        }
        append("</table>\n")
    }

    fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun unescapeAttribute(value: String): String =
        value.replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

    /** Draws a [CalloutBlock] as a titled, coloured box. */
    private class CalloutHtmlRenderer(private val context: HtmlNodeRendererContext) : NodeRenderer {

        override fun getNodeTypes(): Set<Class<out Node>> = setOf(CalloutBlock::class.java)

        override fun render(node: Node) {
            val callout = node as CalloutBlock
            val writer = context.writer
            writer.line()
            writer.tag("div", mapOf("class" to "callout callout-${callout.type.name.lowercase()}"))
            writer.tag("p", mapOf("class" to "callout-title"))
            writer.text("${callout.type.icon} ${callout.type.title}")
            writer.tag("/p")
            var child = node.firstChild
            while (child != null) {
                val next = child.next
                context.render(child)
                child = next
            }
            writer.tag("/div")
            writer.line()
        }
    }

    private const val STYLE = """
body { font-family: sans-serif; font-size: 11pt; line-height: 1.5; color: #1b1b1f; margin: 0; }
h1, h2, h3, h4, h5, h6 { line-height: 1.25; margin: 1.2em 0 0.4em; page-break-after: avoid; }
h1 { font-size: 1.9em; border-bottom: 1px solid #ccc; padding-bottom: 0.2em; }
h2 { font-size: 1.5em; border-bottom: 1px solid #e3e3e3; padding-bottom: 0.15em; }
h3 { font-size: 1.25em; }
p, ul, ol, blockquote, pre, table { margin: 0.6em 0; }
a { color: #1b5e9c; }
code { font-family: monospace; background: #f1f1f3; padding: 0.1em 0.3em; border-radius: 3px; font-size: 0.92em; }
pre { background: #f1f1f3; padding: 0.7em 0.9em; border-radius: 4px; white-space: pre-wrap; word-wrap: break-word; page-break-inside: avoid; }
pre code { background: none; padding: 0; }
blockquote { margin-left: 0; padding: 0.1em 1em; border-left: 4px solid #c3c3c8; color: #46464c; }
table { border-collapse: collapse; width: 100%; page-break-inside: auto; }
tr { page-break-inside: avoid; }
th, td { border: 1px solid #c3c3c8; padding: 0.35em 0.6em; text-align: left; vertical-align: top; }
th { background: #e8e8ec; }
table.meta { width: auto; margin-bottom: 1.2em; font-size: 0.9em; }
table.meta th { white-space: nowrap; }
img { max-width: 100%; }
hr { border: 0; border-top: 1px solid #c3c3c8; margin: 1.2em 0; }
.callout { border-left: 4px solid; padding: 0.1em 1em; margin: 0.8em 0; page-break-inside: avoid; }
.callout-title { font-weight: bold; margin: 0.4em 0; }
.callout-note { border-color: #0969da; } .callout-note .callout-title { color: #0969da; }
.callout-tip { border-color: #1a7f37; } .callout-tip .callout-title { color: #1a7f37; }
.callout-important { border-color: #8250df; } .callout-important .callout-title { color: #8250df; }
.callout-warning { border-color: #9a6700; } .callout-warning .callout-title { color: #9a6700; }
.callout-caution { border-color: #cf222e; } .callout-caution .callout-title { color: #cf222e; }
"""
}

package com.readmd.app.domain

/** Result of separating a leading YAML front matter block from the markdown body. */
data class FrontMatterSplit(
    /** The YAML between the `---` fences, or null when the document has none. */
    val yaml: String?,
    /** The document without the front matter (the original text when there is none). */
    val body: String,
)

data class FrontMatterEntry(
    val key: String,
    val value: String,
)

/**
 * Detects and reads a leading YAML front matter block. Only the top level is read:
 * `key: value` pairs, inline lists, `- item` lists and wrapped values. Nested maps
 * are shown as their raw text.
 */
object FrontMatter {

    private val KEY_LINE = Regex("^([^\\s:#\\-][^:]*|-[^\\s:][^:]*):(\\s+(.*))?$")

    fun split(markdown: String): FrontMatterSplit {
        val text = markdown.removePrefix("﻿")
        val lines = text.split("\n")
        if (lines.firstOrNull()?.trimEnd() != "---") return FrontMatterSplit(null, markdown)

        val end = (1 until lines.size).firstOrNull {
            val line = lines[it].trimEnd()
            line == "---" || line == "..."
        } ?: return FrontMatterSplit(null, markdown)

        val block = lines.subList(1, end).map { it.trimEnd('\r') }
        // A horizontal rule followed by prose and another rule is not front matter.
        if (block.none { KEY_LINE.matches(it) }) return FrontMatterSplit(null, markdown)

        val body = lines.drop(end + 1).joinToString("\n").removePrefix("\r")
        return FrontMatterSplit(yaml = block.joinToString("\n"), body = body)
    }

    fun entries(yaml: String?): List<FrontMatterEntry> {
        if (yaml.isNullOrBlank()) return emptyList()

        val result = ArrayList<FrontMatterEntry>()
        var key: String? = null
        var inline = ""
        val continuation = ArrayList<String>()

        fun finish() {
            val k = key ?: return
            val items = continuation.map { it.trim() }
            val value = when {
                items.isNotEmpty() && items.all { it.startsWith("- ") || it == "-" } ->
                    items.joinToString(", ") { unquote(it.removePrefix("-").trim()) }
                else -> {
                    val head = if (inline == "|" || inline == ">" || inline == "|-" || inline == ">-") "" else inline
                    (listOf(head) + items).filter { it.isNotEmpty() }.joinToString(" ")
                }
            }
            result += FrontMatterEntry(k.trim(), value)
            key = null
            inline = ""
            continuation.clear()
        }

        for (rawLine in yaml.split("\n")) {
            val line = rawLine.trimEnd('\r')
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val topLevel = !line.first().isWhitespace() && !line.startsWith("- ")
            val match = if (topLevel) KEY_LINE.matchEntire(line) else null
            if (match != null) {
                finish()
                key = match.groupValues[1]
                inline = inlineValue(match.groupValues[3].trim())
            } else if (key != null) {
                continuation += line
            }
        }
        finish()
        return result
    }

    private fun inlineValue(raw: String): String {
        if (raw.startsWith("[") && raw.endsWith("]")) {
            return raw.substring(1, raw.length - 1).split(',').joinToString(", ") { unquote(it.trim()) }
        }
        return unquote(raw)
    }

    private fun unquote(value: String): String =
        if (value.length >= 2 && (value.first() == '"' || value.first() == '\'') && value.last() == value.first()) {
            value.substring(1, value.length - 1)
        } else {
            value
        }
}

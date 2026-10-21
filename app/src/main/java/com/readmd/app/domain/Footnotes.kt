package com.readmd.app.domain

/**
 * Turns markdown footnotes (`text[^1]` with a `[^1]: definition` line) into plain links and a
 * "Footnotes" list at the end of the document, because the renderer has no footnote support.
 * Each reference becomes a superscript number linking to its definition, and each definition
 * ends with a link back to the first reference.
 */
object Footnotes {
    const val REFERENCE_PREFIX = "readmd-footnote:"
    const val BACK_PREFIX = "readmd-footnoteback:"

    private val DEFINITION = Regex("^ {0,3}\\[\\^([^\\]\\s]+)]:[ \\t]*(.*)$")
    private val REFERENCE = Regex("\\[\\^([^\\]\\s]+)]")
    private val OPENING_FENCE = Regex("^\\s*(`{3,}|~{3,})")
    private const val SUPERSCRIPT = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    /** The footnote number a reference link points at (its definition), or null for other links. */
    fun referenceNumber(link: String): Int? =
        if (link.startsWith(REFERENCE_PREFIX)) link.removePrefix(REFERENCE_PREFIX).toIntOrNull() else null

    /** The footnote number a "back" link belongs to, or null for other links. */
    fun backNumber(link: String): Int? =
        if (link.startsWith(BACK_PREFIX)) link.removePrefix(BACK_PREFIX).toIntOrNull() else null

    fun prepare(markdown: String): String {
        if (!markdown.contains("[^")) return markdown

        // Pass 1: pull definition lines out of the text (code is never touched).
        val definitions = LinkedHashMap<String, String>()
        val body = ArrayList<Pair<String, Boolean>>() // line, isCode
        var fenceChar = ' '
        var fenceLength = 0
        var lastDefinition: String? = null

        for (line in markdown.split("\n")) {
            val bare = line.trimEnd('\r')
            if (fenceLength > 0) {
                if (bare.trim().let { it.length >= fenceLength && it.all { c -> c == fenceChar } }) fenceLength = 0
                body += line to true
                lastDefinition = null
                continue
            }
            val fence = OPENING_FENCE.find(bare)?.groupValues?.get(1)
            if (fence != null) {
                fenceChar = fence[0]
                fenceLength = fence.length
                body += line to true
                lastDefinition = null
                continue
            }
            val definition = DEFINITION.find(bare)
            if (definition != null) {
                val id = definition.groupValues[1]
                definitions.putIfAbsent(id, definition.groupValues[2].trim())
                lastDefinition = id
                continue
            }
            val continuation = lastDefinition
            if (continuation != null && bare.isNotBlank() && (bare.startsWith("  ") || bare.startsWith("\t"))) {
                definitions[continuation] = (definitions[continuation] + " " + bare.trim()).trim()
                continue
            }
            lastDefinition = null
            body += line to false
        }
        if (definitions.isEmpty()) return markdown

        // Pass 2: number the references in order of appearance and turn them into links.
        val numbers = LinkedHashMap<String, Int>()
        val converted = body.map { (line, isCode) ->
            if (isCode) line else replaceReferences(line, definitions.keys, numbers)
        }
        if (numbers.isEmpty()) return markdown

        val text = converted.toMutableList()
        while (text.isNotEmpty() && text.last().isBlank()) text.removeAt(text.lastIndex)
        text += ""
        text += "---"
        text += ""
        text += "**Footnotes**"
        text += ""
        for ((id, number) in numbers) {
            text += "$number. ${definitions.getValue(id)} [↩]($BACK_PREFIX$number)"
        }
        return text.joinToString("\n")
    }

    private fun replaceReferences(
        line: String,
        defined: Set<String>,
        numbers: MutableMap<String, Int>,
    ): String {
        if (!line.contains("[^")) return line
        val sb = StringBuilder(line.length + 16)
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '`') {
                var n = 0
                while (i + n < line.length && line[i + n] == '`') n++
                val close = line.indexOf("`".repeat(n), i + n)
                val end = if (close >= 0) close + n else i + n
                sb.append(line, i, end)
                i = end
                continue
            }
            if (c == '\\' && i + 1 < line.length) {
                sb.append(c).append(line[i + 1])
                i += 2
                continue
            }
            val match = if (c == '[') REFERENCE.matchAt(line, i) else null
            if (match != null && match.groupValues[1] in defined) {
                val number = numbers.getOrPut(match.groupValues[1]) { numbers.size + 1 }
                sb.append('[').append(superscript(number)).append("](").append(REFERENCE_PREFIX).append(number).append(')')
                i = match.range.last + 1
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun superscript(number: Int): String =
        number.toString().map { SUPERSCRIPT[it - '0'] }.joinToString("")
}

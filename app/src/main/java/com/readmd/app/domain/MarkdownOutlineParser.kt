package com.readmd.app.domain

/**
 * Lightweight markdown heading extractor, independent from the rendering
 * library so it stays fast and unit-testable. Handles ATX headings
 * (`# Heading`), setext headings (`Heading` followed by `===` or `---`),
 * and ignores anything inside fenced code blocks.
 */
object MarkdownOutlineParser {

    private val atx = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val fenceOpen = Regex("^(`{3,}|~{3,})(.*)$")
    private val setextLine = Regex("^(=+|-+)\\s*$")

    fun parse(markdown: String): List<HeadingNode> {
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val result = ArrayList<HeadingNode>()

        var inFence = false
        var fenceChar = ' '
        var fenceLength = 0

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()

            val fence = fenceOpen.find(trimmed)
            if (fence != null) {
                val marker = fence.groupValues[1]
                if (!inFence) {
                    inFence = true
                    fenceChar = marker[0]
                    fenceLength = marker.length
                } else if (marker[0] == fenceChar && marker.length >= fenceLength && fence.groupValues[2].isBlank()) {
                    inFence = false
                }
                i++
                continue
            }

            if (inFence) {
                i++
                continue
            }

            // ATX: "# Heading"
            val atxMatch = atx.find(trimmed)
            if (atxMatch != null) {
                result += HeadingNode(atxMatch.groupValues[1].length, clean(atxMatch.groupValues[2]))
                i++
                continue
            }

            // Setext: "Heading" underlined with "===" or "---"
            if (line.isNotBlank() && !trimmed.startsWith("#") && i + 1 < lines.size) {
                val next = lines[i + 1]
                val nextTrimmed = next.trim()
                if (setextLine.matches(nextTrimmed) && !next.startsWith(" ") && !next.startsWith("\t")) {
                    val level = if (nextTrimmed[0] == '=') 1 else 2
                    result += HeadingNode(level, clean(line.trim()))
                    i += 2
                    continue
                }
            }

            i++
        }

        return result
    }

    /** Strips common inline markdown markers so TOC entries read cleanly. */
    private fun clean(text: String): String = text
        .replace(Regex("!\\[([^]]*)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("(\\*\\*|__|~~|`|\\*|_)"), "")
        .trim()
}

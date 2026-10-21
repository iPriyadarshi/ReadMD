package com.readmd.app.domain

/**
 * Rewrites single-dollar inline math into the double-dollar form the renderer understands:
 * `$x^2$` becomes `$$x^2$$`. Prices such as "$5 and $10" are left alone, using the usual
 * Pandoc rules: the opening `$` must be followed by a non-space, the closing `$` must follow a
 * non-space and must not be followed by a digit. Code (fenced blocks and inline code),
 * escaped `\$`, existing `$$` math and multi-line `$$` blocks are never touched.
 */
object MathMarkup {

    private val OPENING_FENCE = Regex("^\\s*(`{3,}|~{3,})")

    fun prepare(markdown: String): String {
        if ('$' !in markdown) return markdown

        val out = ArrayList<String>()
        var fenceChar = ' '
        var fenceLength = 0
        var inMathBlock = false

        for (line in markdown.split("\n")) {
            val bare = line.trimEnd('\r')
            val trimmed = bare.trim()
            when {
                fenceLength > 0 -> {
                    if (trimmed.length >= fenceLength && trimmed.all { it == fenceChar }) fenceLength = 0
                    out += line
                }
                inMathBlock -> {
                    if (trimmed == "$$") inMathBlock = false
                    out += line
                }
                else -> {
                    val fence = OPENING_FENCE.find(bare)?.groupValues?.get(1)
                    if (fence != null) {
                        fenceChar = fence[0]
                        fenceLength = fence.length
                        out += line
                    } else if (trimmed == "$$") {
                        inMathBlock = true
                        out += line
                    } else {
                        out += convertLine(line)
                    }
                }
            }
        }
        return out.joinToString("\n")
    }

    private fun convertLine(line: String): String {
        if ('$' !in line) return line
        val sb = StringBuilder(line.length + 8)
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\\' -> {
                    sb.append(c)
                    if (i + 1 < line.length) sb.append(line[i + 1])
                    i += 2
                }
                c == '`' -> {
                    var n = 0
                    while (i + n < line.length && line[i + n] == '`') n++
                    val ticks = "`".repeat(n)
                    val close = line.indexOf(ticks, i + n)
                    val end = if (close >= 0) close + n else i + n
                    sb.append(line, i, end)
                    i = end
                }
                c == '$' && i + 1 < line.length && line[i + 1] == '$' -> {
                    // Existing double-dollar math: copy through its closing pair if on this line.
                    val close = line.indexOf("$$", i + 2)
                    val end = if (close >= 0) close + 2 else i + 2
                    sb.append(line, i, end)
                    i = end
                }
                c == '$' -> {
                    val close = closingDollar(line, i)
                    if (close < 0) {
                        sb.append(c)
                        i++
                    } else {
                        sb.append("$$").append(line, i + 1, close).append("$$")
                        i = close + 1
                    }
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    /** Index of the `$` closing the inline math opened at [open], or -1 if it isn't math. */
    private fun closingDollar(line: String, open: Int): Int {
        val first = line.getOrNull(open + 1) ?: return -1
        if (first.isWhitespace()) return -1
        var j = open + 1
        while (j < line.length) {
            val ch = line[j]
            if (ch == '\\') {
                j += 2
                continue
            }
            if (ch == '$') {
                val before = line[j - 1]
                val after = line.getOrNull(j + 1)
                val validClose = j > open + 1 &&
                    !before.isWhitespace() &&
                    after != '$' &&
                    (after == null || !after.isDigit())
                return if (validClose) j else -1
            }
            j++
        }
        return -1
    }
}

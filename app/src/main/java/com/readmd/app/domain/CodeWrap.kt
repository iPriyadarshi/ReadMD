package com.readmd.app.domain

/**
 * How long lines in fenced code blocks are handled. The reader draws a document into one
 * text view, which always wraps, so "scroll" adds a link that opens the block in a viewer
 * where lines are never wrapped and scroll sideways.
 */
enum class CodeWrap(val id: String, val label: String) {
    WRAP("wrap", "Wrap long lines"),
    SCROLL("scroll", "Scroll long lines");

    companion object {
        val DEFAULT = WRAP

        /** Lines longer than this are likely to wrap on a phone in portrait. */
        const val LONG_LINE_CHARS = 48

        fun fromId(id: String?): CodeWrap = entries.firstOrNull { it.id == id } ?: DEFAULT

        fun hasLongLine(code: String): Boolean = code.lineSequence().any { it.length > LONG_LINE_CHARS }
    }
}

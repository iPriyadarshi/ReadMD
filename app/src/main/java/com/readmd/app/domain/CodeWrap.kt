package com.readmd.app.domain

import androidx.annotation.StringRes
import com.readmd.app.R

/**
 * How long lines in fenced code blocks are handled. The reader draws a document into one
 * text view, which always wraps, so "scroll" adds a link that opens the block in a viewer
 * where lines are never wrapped and scroll sideways.
 */
enum class CodeWrap(val id: String, @StringRes val labelRes: Int) {
    WRAP("wrap", R.string.code_wrap_wrap),
    SCROLL("scroll", R.string.code_wrap_scroll);

    companion object {
        val DEFAULT = WRAP

        /** Lines longer than this are likely to wrap on a phone in portrait. */
        const val LONG_LINE_CHARS = 48

        fun fromId(id: String?): CodeWrap = entries.firstOrNull { it.id == id } ?: DEFAULT

        fun hasLongLine(code: String): Boolean = code.lineSequence().any { it.length > LONG_LINE_CHARS }
    }
}

package com.readmd.app.domain

import kotlin.math.ceil

/** Reading time estimates based on word count and how far the user has scrolled. */
object ReadingTime {
    const val WORDS_PER_MINUTE = 200

    private val WHITESPACE = Regex("\\s+")

    fun wordCount(text: String): Int =
        text.split(WHITESPACE).count { it.isNotEmpty() }

    /** Whole minutes left (rounded up) given [progress] in 0..1; 0 only when nothing is left. */
    fun minutesLeft(words: Int, progress: Float): Int {
        val remaining = words * (1f - progress.coerceIn(0f, 1f))
        return ceil(remaining / WORDS_PER_MINUTE).toInt()
    }
}

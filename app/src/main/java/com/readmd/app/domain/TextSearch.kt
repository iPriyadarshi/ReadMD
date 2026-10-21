package com.readmd.app.domain

/** First-match context for a document that contains the query. */
data class TextMatch(
    /** One-line excerpt around the first match, with ellipses where it was cut. */
    val snippet: String,
    /** Total number of case-insensitive occurrences of the query. */
    val count: Int,
)

object TextSearch {
    const val MIN_QUERY_LENGTH = 2

    private val WHITESPACE = Regex("\\s+")

    /**
     * Case-insensitive search of [text] for [query]; returns null when there is no match.
     * The snippet shows up to [radius] characters either side of the first match.
     */
    fun find(text: String, query: String, radius: Int = 40): TextMatch? {
        if (query.isBlank()) return null
        val first = text.indexOf(query, ignoreCase = true)
        if (first < 0) return null

        var count = 0
        var index = first
        while (index >= 0) {
            count++
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }

        val start = (first - radius).coerceAtLeast(0)
        val end = (first + query.length + radius).coerceAtMost(text.length)
        val excerpt = text.substring(start, end).replace(WHITESPACE, " ").trim()
        val snippet = buildString {
            if (start > 0) append('…')
            append(excerpt)
            if (end < text.length) append('…')
        }
        return TextMatch(snippet = snippet, count = count)
    }
}

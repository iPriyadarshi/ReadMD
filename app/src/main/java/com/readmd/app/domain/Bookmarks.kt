package com.readmd.app.domain

data class Bookmark(
    val docId: String,
    /** Scroll position in thousandths of the scroll range, so it survives text size changes. */
    val permille: Int,
    val label: String,
    val createdAt: Long,
)

/**
 * Pure helpers for persisted bookmarks. Each entry is stored as
 * "docId\u001fpermille\u001fcreatedAt\u001flabel" in a string set.
 */
object Bookmarks {
    const val MAX_PER_DOC = 50

    private const val SEP = "\u001f"

    fun forDoc(entries: Set<String>, docId: String): List<Bookmark> =
        entries.mapNotNull(::parse).filter { it.docId == docId }.sortedBy { it.permille }

    /** Adds [bookmark], replacing one at the same position and dropping the oldest beyond [MAX_PER_DOC]. */
    fun add(entries: Set<String>, bookmark: Bookmark, maxPerDoc: Int = MAX_PER_DOC): Set<String> {
        val position = bookmark.permille.coerceIn(0, ReadingPositions.FULL)
        val clean = bookmark.copy(permille = position, label = bookmark.label.replace(SEP, " "))
        val others = entries.filterNot {
            parse(it)?.let { b -> b.docId == clean.docId && b.permille == position } ?: false
        }
        val mine = (others.mapNotNull(::parse).filter { it.docId == clean.docId } + clean)
            .sortedByDescending { it.createdAt }
            .take(maxPerDoc)
            .toSet()
        val rest = others.filterNot { parse(it)?.docId == clean.docId }
        return (rest + mine.map(::encode)).toSet()
    }

    fun remove(entries: Set<String>, docId: String, permille: Int): Set<String> =
        entries.filterNot {
            parse(it)?.let { b -> b.docId == docId && b.permille == permille } ?: false
        }.toSet()

    /**
     * Label for a bookmark at scroll offset [y]: the closest heading at or above it
     * (a heading within [slack] below [y] counts too), or null if none precedes it.
     */
    fun labelFor(headingOffsets: List<Pair<String, Float>>, y: Float, slack: Float = 20f): String? =
        headingOffsets.filter { it.second <= y + slack }.maxByOrNull { it.second }?.first

    private fun encode(b: Bookmark) = "${b.docId}$SEP${b.permille}$SEP${b.createdAt}$SEP${b.label}"

    private fun parse(raw: String): Bookmark? {
        val parts = raw.split(SEP, limit = 4)
        if (parts.size != 4) return null
        val permille = parts[1].toIntOrNull() ?: return null
        return Bookmark(
            docId = parts[0],
            permille = permille,
            label = parts[3],
            createdAt = parts[2].toLongOrNull() ?: 0L,
        )
    }
}

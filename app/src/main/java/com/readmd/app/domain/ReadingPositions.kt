package com.readmd.app.domain

/**
 * Pure helpers for the persisted per-document reading positions. Each entry is
 * stored as "timestamp\u001fpermille\u001fdocId" in a string set; the position is
 * the scroll fraction in thousandths so it survives text size and width changes.
 */
object ReadingPositions {
    const val MAX_ENTRIES = 100
    const val FULL = 1000

    private const val SEP = "\u001f"

    fun get(entries: Set<String>, docId: String): Int? =
        entries.firstNotNullOfOrNull { raw ->
            val parts = raw.split(SEP, limit = 3)
            if (parts.size == 3 && parts[2] == docId) parts[1].toIntOrNull()?.coerceIn(0, FULL) else null
        }

    /** Replaces any entry for [docId] and drops the oldest entries beyond [max]. */
    fun upsert(
        entries: Set<String>,
        docId: String,
        permille: Int,
        now: Long,
        max: Int = MAX_ENTRIES,
    ): Set<String> {
        val kept = entries.filterNot { it.split(SEP, limit = 3).getOrNull(2) == docId }
        val added = kept + "$now$SEP${permille.coerceIn(0, FULL)}$SEP$docId"
        return added
            .sortedByDescending { it.substringBefore(SEP).toLongOrNull() ?: 0L }
            .take(max)
            .toSet()
    }
}

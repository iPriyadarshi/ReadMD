package com.readmd.app.domain

data class FavoriteEntry(
    val id: String,
    val name: String,
    val addedAt: Long,
)

/**
 * Pure helpers for the persisted favorites. Each entry is stored as
 * "addedAt\u001fname\u001fid" in a string set; ordering is applied on read.
 */
object Favorites {
    private const val SEP = "\u001f"

    fun parse(entries: Set<String>): List<FavoriteEntry> =
        entries.mapNotNull { raw ->
            val parts = raw.split(SEP, limit = 3)
            if (parts.size != 3) {
                null
            } else {
                FavoriteEntry(id = parts[2], name = parts[1], addedAt = parts[0].toLongOrNull() ?: 0L)
            }
        }.sortedByDescending { it.addedAt }

    /** Adds [id] (replacing any existing entry for it, so the name stays current). */
    fun add(entries: Set<String>, id: String, name: String, now: Long): Set<String> =
        remove(entries, id) + "$now$SEP$name$SEP$id"

    fun remove(entries: Set<String>, id: String): Set<String> =
        entries.filterNot { it.split(SEP, limit = 3).getOrNull(2) == id }.toSet()
}

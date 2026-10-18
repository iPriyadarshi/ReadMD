package com.readmd.app.data

/** Sorting options for the picked-folder file list. */
enum class FolderSort(val id: String, val label: String) {
    NAME_ASC("NAME_ASC", "Name A\u2013Z"),
    NAME_DESC("NAME_DESC", "Name Z\u2013A"),
    NEWEST("NEWEST", "Newest first"),
    OLDEST("OLDEST", "Oldest first");

    fun apply(entries: List<FolderEntry>): List<FolderEntry> = when (this) {
        NAME_ASC -> entries.sortedBy { it.name.lowercase() }
        NAME_DESC -> entries.sortedByDescending { it.name.lowercase() }
        NEWEST -> entries.sortedByDescending { it.lastModified }
        OLDEST -> entries.sortedBy { it.lastModified }
    }

    companion object {
        /** Tolerant lookup for persisted values; falls back to [NAME_ASC]. */
        fun fromId(id: String?): FolderSort =
            entries.firstOrNull { it.id == id } ?: NAME_ASC
    }
}

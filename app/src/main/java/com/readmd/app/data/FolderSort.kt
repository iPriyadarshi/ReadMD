package com.readmd.app.data

import androidx.annotation.StringRes
import com.readmd.app.R

/** Sorting options for the picked-folder file list. */
enum class FolderSort(val id: String, @StringRes val labelRes: Int) {
    NAME_ASC("NAME_ASC", R.string.sort_name_asc),
    NAME_DESC("NAME_DESC", R.string.sort_name_desc),
    NEWEST("NEWEST", R.string.sort_newest),
    OLDEST("OLDEST", R.string.sort_oldest);

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

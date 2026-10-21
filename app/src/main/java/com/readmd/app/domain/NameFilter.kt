package com.readmd.app.domain

/** Live filtering of document lists by file name. */
object NameFilter {

    /** Items whose name contains [query] (case-insensitive, surrounding spaces ignored); all items when blank. */
    fun <T> filter(items: List<T>, query: String, nameOf: (T) -> String): List<T> {
        val needle = query.trim()
        if (needle.isEmpty()) return items
        return items.filter { nameOf(it).contains(needle, ignoreCase = true) }
    }
}

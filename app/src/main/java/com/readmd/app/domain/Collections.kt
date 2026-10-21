package com.readmd.app.domain

data class DocCollection(
    val id: String,
    val name: String,
)

data class CollectionItem(
    val collectionId: String,
    val docId: String,
    val name: String,
)

/**
 * Pure helpers for user-defined collections. Collections are stored as "id\u001fname"
 * and their documents as "collectionId\u001fname\u001fdocId", each in a string set.
 */
object Collections {
    const val MAX_NAME_LENGTH = 40

    private const val SEP = "\u001f"

    fun normalizeName(raw: String): String =
        raw.replace(SEP, " ").trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH).trim()

    fun parseCollections(entries: Set<String>): List<DocCollection> =
        entries.mapNotNull { raw ->
            val parts = raw.split(SEP, limit = 2)
            if (parts.size == 2) DocCollection(parts[0], parts[1]) else null
        }.sortedBy { it.name.lowercase() }

    fun parseItems(entries: Set<String>): List<CollectionItem> =
        entries.mapNotNull { raw ->
            val parts = raw.split(SEP, limit = 3)
            if (parts.size == 3) CollectionItem(collectionId = parts[0], name = parts[1], docId = parts[2]) else null
        }

    /** True when another collection already uses [name] (case-insensitive), ignoring [exceptId]. */
    fun nameTaken(collections: List<DocCollection>, name: String, exceptId: String? = null): Boolean =
        collections.any { it.id != exceptId && it.name.equals(name, ignoreCase = true) }

    fun create(entries: Set<String>, id: String, name: String): Set<String> =
        entries + "$id$SEP${normalizeName(name)}"

    fun rename(entries: Set<String>, id: String, name: String): Set<String> =
        entries.map { raw ->
            if (raw.substringBefore(SEP) == id) "$id$SEP${normalizeName(name)}" else raw
        }.toSet()

    fun delete(entries: Set<String>, id: String): Set<String> =
        entries.filterNot { it.substringBefore(SEP) == id }.toSet()

    /** Removes every document that belonged to the deleted collection [id]. */
    fun deleteItems(items: Set<String>, id: String): Set<String> =
        items.filterNot { it.substringBefore(SEP) == id }.toSet()

    fun assign(items: Set<String>, collectionId: String, docId: String, name: String): Set<String> =
        unassign(items, collectionId, docId) + "$collectionId$SEP${name.replace(SEP, " ")}$SEP$docId"

    fun unassign(items: Set<String>, collectionId: String, docId: String): Set<String> =
        items.filterNot {
            val parts = it.split(SEP, limit = 3)
            parts.size == 3 && parts[0] == collectionId && parts[2] == docId
        }.toSet()

    fun docsIn(items: List<CollectionItem>, collectionId: String): List<CollectionItem> =
        items.filter { it.collectionId == collectionId }.sortedBy { it.name.lowercase() }

    fun collectionIdsOf(items: List<CollectionItem>, docId: String): Set<String> =
        items.filter { it.docId == docId }.mapTo(HashSet()) { it.collectionId }
}

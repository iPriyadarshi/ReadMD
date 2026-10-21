package com.readmd.app.domain

import java.util.UUID

/** Names and ids for the launcher shortcuts that open recent documents. */
object ShortcutLabels {
    /** Launchers show only a few shortcuts; the system caps the real number per app. */
    const val MAX_SHORTCUTS = 4

    private const val MAX_LABEL = 25

    /** The document name without its markdown extension, shortened to fit a launcher label. */
    fun label(name: String): String {
        val base = name.trim().removeSuffix(".md").removeSuffix(".markdown").removeSuffix(".MD").trim()
            .ifEmpty { name.trim() }
            .ifEmpty { "Document" }
        return if (base.length <= MAX_LABEL) base else base.take(MAX_LABEL - 1).trimEnd() + "…"
    }

    /** A stable id for a document, so republishing the same recents replaces rather than duplicates. */
    fun id(docId: String): String = "doc-" + UUID.nameUUIDFromBytes(docId.toByteArray(Charsets.UTF_8))
}

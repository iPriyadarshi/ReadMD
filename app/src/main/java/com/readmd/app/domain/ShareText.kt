package com.readmd.app.domain

/** What gets handed to the system share sheet for a document. */
object ShareText {

    /**
     * Intents travel through a size-limited system call (about 1 MB for everything in the
     * transaction), so very large documents can't be shared as plain text.
     */
    const val MAX_CHARS = 200_000

    fun fits(text: String): Boolean = text.length <= MAX_CHARS

    /** The title shown as the share subject: the document name without its markdown extension. */
    fun subject(title: String): String =
        title.trim().removeSuffix(".md").removeSuffix(".markdown").removeSuffix(".MD").trim().ifEmpty { "Document" }
}

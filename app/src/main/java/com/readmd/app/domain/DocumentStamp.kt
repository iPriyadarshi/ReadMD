package com.readmd.app.domain

/**
 * A cheap fingerprint of a file on disk: when it was last modified and how big it is.
 * Either part may be unknown for providers that don't report it.
 */
data class DocumentStamp(
    val lastModified: Long?,
    val size: Long?,
) {
    /** True when the provider reported neither value, so changes can't be detected. */
    val isEmpty: Boolean get() = lastModified == null && size == null

    companion object {
        /**
         * True when [current] shows the file was modified after [known] was taken. Nothing is
         * reported when either stamp is missing or empty, so providers that give no
         * information never cause reloads.
         */
        fun changed(known: DocumentStamp?, current: DocumentStamp?): Boolean {
            if (known == null || current == null || known.isEmpty || current.isEmpty) return false
            return known != current
        }
    }
}

package com.readmd.app.domain

/** File names offered when exporting a document. */
object ExportNames {

    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    /** [title] without its markdown extension or characters file systems reject, plus [extension]. */
    fun fileName(title: String, extension: String): String {
        val base = title
            .trim()
            .removeSuffix(".md").removeSuffix(".markdown").removeSuffix(".MD")
            .replace(ILLEGAL, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(100)
            .trim()
        return "${base.ifEmpty { "document" }}.$extension"
    }
}

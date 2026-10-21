package com.readmd.app.domain

/** File names for documents created from content shared into the app. */
object SharedNames {

    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val MARKDOWN_EXTENSIONS = listOf(".md", ".markdown", ".mdown", ".txt")

    /** Name for shared text: the subject when there is one, otherwise "Shared text". */
    fun forText(subject: String?): String {
        val base = subject.orEmpty().replace(ILLEGAL, " ").replace(Regex("\\s+"), " ").trim().trim('.').take(60).trim()
        return "${base.ifEmpty { "Shared text" }}.md"
    }

    /** Name for a shared file: its own name, made safe, with a markdown extension if it has none. */
    fun forFile(displayName: String?): String {
        val base = displayName.orEmpty().replace(ILLEGAL, " ").replace(Regex("\\s+"), " ").trim().trim('.').take(100).trim()
        if (base.isEmpty()) return "Shared file.md"
        return if (MARKDOWN_EXTENSIONS.any { base.endsWith(it, ignoreCase = true) }) base else "$base.md"
    }

    /** [wanted] if unused, otherwise "name (2).md", "name (3).md"... so earlier imports are kept. */
    fun unique(wanted: String, taken: Set<String>): String {
        if (wanted !in taken) return wanted
        val dot = wanted.lastIndexOf('.')
        val stem = if (dot > 0) wanted.substring(0, dot) else wanted
        val extension = if (dot > 0) wanted.substring(dot) else ""
        var counter = 2
        while ("$stem ($counter)$extension" in taken) counter++
        return "$stem ($counter)$extension"
    }
}

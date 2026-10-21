package com.readmd.app.domain

import java.net.URLDecoder

/** Resolves links between markdown files that live in the same picked folder tree. */
object RelativeLink {

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

    /** True for links that point at another file relative to this one (not a URL, anchor or absolute path). */
    fun isRelativeFileLink(link: String): Boolean {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("#") || trimmed.startsWith("/") || trimmed.startsWith("//")) return false
        return !SCHEME.containsMatchIn(trimmed)
    }

    fun isMarkdown(path: String): Boolean =
        path.endsWith(".md", ignoreCase = true) || path.endsWith(".markdown", ignoreCase = true)

    /**
     * Resolves [link] against the document id of the current file. Ids on hierarchical
     * providers look like "root:dir/sub/file.md"; the link is applied to the directory
     * part. Returns null when the id is not hierarchical, the link has no path, or the
     * link climbs above the provider root.
     */
    fun resolve(currentDocId: String, link: String): String? {
        val colon = currentDocId.indexOf(':')
        if (colon < 0) return null
        val prefix = currentDocId.substring(0, colon + 1)

        val rawPath = link.trim().substringBefore('#').substringBefore('?')
        if (rawPath.isEmpty()) return null
        val path = runCatching { URLDecoder.decode(rawPath.replace("+", "%2B"), "UTF-8") }
            .getOrDefault(rawPath)

        val segments = currentDocId.substring(colon + 1)
            .split('/')
            .filter { it.isNotEmpty() }
            .dropLast(1)
            .toMutableList()
        for (part in path.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return null else segments.removeAt(segments.lastIndex)
                else -> segments += part
            }
        }
        if (segments.isEmpty()) return null
        return prefix + segments.joinToString("/")
    }
}

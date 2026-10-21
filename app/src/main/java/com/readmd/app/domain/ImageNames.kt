package com.readmd.app.domain

import java.net.URLDecoder

/** Readable names for images in "image not found" placeholders. */
object ImageNames {

    /**
     * The file name part of an image address, e.g. `a b.png` for
     * `content://auth/tree/primary%3ADocs/document/primary%3ADocs%2Fimg%2Fa%20b.png`.
     * Falls back to the whole (decoded) address when there is no name to show.
     */
    fun displayName(url: String): String {
        val decoded = runCatching { URLDecoder.decode(url.replace("+", "%2B"), "UTF-8") }.getOrDefault(url)
        val withoutQuery = decoded.substringBefore('#').substringBefore('?')
        val name = withoutQuery.trimEnd('/').substringAfterLast('/').substringAfterLast(':')
        return name.ifEmpty { decoded }
    }
}

package com.readmd.app.domain

/** GitHub-style anchors for headings, as used in `[text](#anchor)` links. */
object HeadingAnchors {

    /**
     * The anchor GitHub gives a heading: lower-case, with everything except letters, digits,
     * spaces, hyphens and underscores removed, and spaces turned into hyphens.
     */
    fun slug(heading: String): String {
        val out = StringBuilder()
        heading.lowercase().codePoints().forEach { codePoint ->
            when {
                codePoint == ' '.code -> out.append('-')
                codePoint == '-'.code || codePoint == '_'.code || Character.isLetterOrDigit(codePoint) ->
                    out.appendCodePoint(codePoint)
            }
        }
        return out.toString()
    }

    /**
     * Anchors for [headings] in order, with repeated names numbered like GitHub does:
     * `intro`, `intro-1`, `intro-2`. Each anchor starts with `#`.
     */
    fun anchors(headings: List<HeadingNode>): List<String> {
        val seen = HashMap<String, Int>()
        return headings.map { heading ->
            val slug = slug(heading.text)
            val count = seen.getOrDefault(slug, 0)
            seen[slug] = count + 1
            if (count == 0) "#$slug" else "#$slug-$count"
        }
    }
}

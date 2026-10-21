package com.readmd.app.domain

/** A markdown document with each mermaid block preceded by a "View diagram" link. */
data class MermaidPrepared(
    val markdown: String,
    /** Source of each mermaid block, indexed by the number in its link. */
    val diagrams: List<String>,
)

object MermaidBlocks {
    const val LINK_PREFIX = "readmd-mermaid:"

    private val OPENING_FENCE = Regex("^(\\s*)(`{3,}|~{3,})\\s*([^\\s`]*)")

    /** Index of the diagram a "View diagram" link points at, or null for any other link. */
    fun diagramIndex(link: String): Int? =
        if (link.startsWith(LINK_PREFIX)) link.removePrefix(LINK_PREFIX).toIntOrNull() else null

    /**
     * Finds fenced code blocks whose info string is "mermaid" and inserts a tappable link
     * line before each, leaving the block itself (shown as plain code) untouched. Fences
     * nested inside other fenced blocks are ignored.
     */
    fun prepare(markdown: String): MermaidPrepared {
        if (!markdown.contains("mermaid", ignoreCase = true)) return MermaidPrepared(markdown, emptyList())

        val lines = markdown.split("\n")
        val out = ArrayList<String>(lines.size + 8)
        val diagrams = ArrayList<String>()

        var fenceChar = ' '
        var fenceLength = 0
        var current: StringBuilder? = null

        for (line in lines) {
            val trimmedEnd = line.trimEnd('\r')
            if (fenceLength == 0) {
                val open = OPENING_FENCE.find(trimmedEnd)
                if (open != null) {
                    val fence = open.groupValues[2]
                    fenceChar = fence[0]
                    fenceLength = fence.length
                    if (open.groupValues[3].equals("mermaid", ignoreCase = true)) {
                        out += "${open.groupValues[1]}[▶ View diagram]($LINK_PREFIX${diagrams.size})"
                        out += ""
                        current = StringBuilder()
                    }
                }
                out += line
            } else {
                val closes = trimmedEnd.trim().let { t ->
                    t.length >= fenceLength && t.all { it == fenceChar }
                }
                if (closes) {
                    current?.let { diagrams += it.toString().trimEnd() }
                    current = null
                    fenceLength = 0
                } else {
                    current?.let { if (it.isNotEmpty()) it.append('\n'); it.append(trimmedEnd) }
                }
                out += line
            }
        }
        // An unclosed mermaid fence runs to the end of the document.
        current?.let { diagrams += it.toString().trimEnd() }

        return MermaidPrepared(out.joinToString("\n"), diagrams)
    }

    fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /**
     * Page that renders [source] with the bundled mermaid script (resolved next to the
     * page, so it must be loaded with the assets folder as its base URL).
     */
    fun html(source: String, dark: Boolean, background: String, foreground: String): String = """
        <!doctype html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body { margin: 0; background: $background; color: $foreground; }
          body { padding: 16px; font-family: sans-serif; }
          pre.mermaid { margin: 0; text-align: center; }
          #error { color: $foreground; white-space: pre-wrap; }
        </style>
        </head>
        <body>
        <pre class="mermaid" id="diagram">${escapeHtml(source)}</pre>
        <div id="error" hidden></div>
        <script src="mermaid.min.js"></script>
        <script>
          mermaid.initialize({ startOnLoad: false, theme: '${if (dark) "dark" else "default"}', securityLevel: 'strict' });
          mermaid.run({ querySelector: '#diagram' }).catch(function (e) {
            document.getElementById('diagram').style.display = 'none';
            var box = document.getElementById('error');
            box.hidden = false;
            box.textContent = "Couldn't render this diagram.\n\n" + (e && e.message ? e.message : e);
          });
        </script>
        </body>
        </html>
    """.trimIndent()
}

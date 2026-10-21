package com.readmd.app.ui.reader

import com.readmd.app.domain.CodeCopy
import io.noties.markwon.AbstractMarkwonPlugin
import org.commonmark.node.Node

/**
 * Puts a "Copy" link above every fenced code block and remembers each block's text, so the
 * link resolver can hand the exact code to the clipboard.
 */
class CodeCopyPlugin : AbstractMarkwonPlugin() {

    private var blocks: List<String> = emptyList()

    override fun beforeRender(node: Node) {
        blocks = CodeCopy.apply(node)
    }

    /** The text of the code block a copy link points at, or null if the link is stale. */
    fun blockAt(index: Int): String? = blocks.getOrNull(index)
}

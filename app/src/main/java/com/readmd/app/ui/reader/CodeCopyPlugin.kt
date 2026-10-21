package com.readmd.app.ui.reader

import com.readmd.app.domain.CodeBlockInfo
import com.readmd.app.domain.CodeCopy
import io.noties.markwon.AbstractMarkwonPlugin
import org.commonmark.node.Node

/**
 * Puts a "Copy" link above every fenced code block and remembers each block's text, so the
 * link resolver can hand the exact code to the clipboard.
 */
class CodeCopyPlugin(private val scrollLongLines: Boolean) : AbstractMarkwonPlugin() {

    private var blocks: List<CodeBlockInfo> = emptyList()

    override fun beforeRender(node: Node) {
        blocks = CodeCopy.apply(node, scrollLongLines)
    }

    /** The code block a copy or scroll link points at, or null if the link is stale. */
    fun blockAt(index: Int): CodeBlockInfo? = blocks.getOrNull(index)
}

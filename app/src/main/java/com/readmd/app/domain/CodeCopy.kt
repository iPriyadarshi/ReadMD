package com.readmd.app.domain

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text

/** One fenced code block found in a document. */
data class CodeBlockInfo(
    /** The block's exact text, without the newline the parser adds before the closing fence. */
    val text: String,
    /** True when a "Scroll" link was added because a line is long enough to wrap. */
    val hasScrollLink: Boolean,
)

/**
 * Adds a "Copy" link line above every fenced code block of a parsed document (plus a
 * "Scroll" link when long lines should open in the sideways-scrolling viewer). Each link
 * carries the block's position, and [apply] returns each block's data in the same order so a
 * tap can look it up.
 */
object CodeCopy {
    const val LINK_PREFIX = "readmd-copy:"
    const val VIEW_PREFIX = "readmd-codeview:"
    const val LINK_TEXT = "⧉ Copy"
    const val VIEW_TEXT = "⤢ Scroll"

    /** Position of the code block a "Copy" link refers to, or null for any other link. */
    fun index(link: String): Int? =
        if (link.startsWith(LINK_PREFIX)) link.removePrefix(LINK_PREFIX).toIntOrNull() else null

    /** Position of the code block a "Scroll" link refers to, or null for any other link. */
    fun viewIndex(link: String): Int? =
        if (link.startsWith(VIEW_PREFIX)) link.removePrefix(VIEW_PREFIX).toIntOrNull() else null

    fun apply(root: Node, scrollLongLines: Boolean = false): List<CodeBlockInfo> {
        val blocks = ArrayList<FencedCodeBlock>()
        root.accept(object : AbstractVisitor() {
            override fun visit(fencedCodeBlock: FencedCodeBlock) {
                blocks += fencedCodeBlock
            }
        })

        return blocks.mapIndexed { index, block ->
            // The parser ends a block's literal with the newline before the closing fence.
            val text = block.literal.removeSuffix("\n")
            val withScroll = scrollLongLines && CodeWrap.hasLongLine(text)

            val line = Paragraph()
            line.appendChild(Link("$LINK_PREFIX$index", null).also { it.appendChild(Text(LINK_TEXT)) })
            if (withScroll) {
                line.appendChild(Text("   "))
                line.appendChild(Link("$VIEW_PREFIX$index", null).also { it.appendChild(Text(VIEW_TEXT)) })
            }
            block.insertBefore(line)
            CodeBlockInfo(text, withScroll)
        }
    }
}

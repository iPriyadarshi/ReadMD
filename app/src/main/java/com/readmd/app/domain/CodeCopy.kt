package com.readmd.app.domain

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text

/**
 * Adds a "Copy" link line above every fenced code block of a parsed document. The link
 * carries the block's position, and [apply] returns each block's exact text in the same
 * order so a tap can look it up.
 */
object CodeCopy {
    const val LINK_PREFIX = "readmd-copy:"
    const val LINK_TEXT = "⧉ Copy"

    /** Position of the code block a "Copy" link refers to, or null for any other link. */
    fun index(link: String): Int? =
        if (link.startsWith(LINK_PREFIX)) link.removePrefix(LINK_PREFIX).toIntOrNull() else null

    fun apply(root: Node): List<String> {
        val blocks = ArrayList<FencedCodeBlock>()
        root.accept(object : AbstractVisitor() {
            override fun visit(fencedCodeBlock: FencedCodeBlock) {
                blocks += fencedCodeBlock
            }
        })

        blocks.forEachIndexed { index, block ->
            val link = Link("$LINK_PREFIX$index", null)
            link.appendChild(Text(LINK_TEXT))
            val line = Paragraph()
            line.appendChild(link)
            block.insertBefore(line)
        }
        // The parser ends a block's literal with the newline before the closing fence.
        return blocks.map { it.literal.removeSuffix("\n") }
    }
}

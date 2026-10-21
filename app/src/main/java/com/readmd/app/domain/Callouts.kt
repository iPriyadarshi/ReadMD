package com.readmd.app.domain

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.CustomBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text

/** GitHub-style alert kinds written as `> [!NOTE]` on the first line of a blockquote. */
enum class CalloutType(
    val marker: String,
    val title: String,
    val icon: String,
    /** ARGB accent used on light backgrounds. */
    val lightColor: Int,
    /** ARGB accent used on dark backgrounds. */
    val darkColor: Int,
) {
    NOTE("NOTE", "Note", "ℹ️", 0xFF0969DA.toInt(), 0xFF4493F8.toInt()),
    TIP("TIP", "Tip", "💡", 0xFF1A7F37.toInt(), 0xFF3FB950.toInt()),
    IMPORTANT("IMPORTANT", "Important", "❗", 0xFF8250DF.toInt(), 0xFFAB7DF8.toInt()),
    WARNING("WARNING", "Warning", "⚠️", 0xFF9A6700.toInt(), 0xFFD29922.toInt()),
    CAUTION("CAUTION", "Caution", "🛑", 0xFFCF222E.toInt(), 0xFFF85149.toInt());

    fun color(dark: Boolean): Int = if (dark) darkColor else lightColor

    companion object {
        private val MARKER = Regex("^\\[!([A-Za-z]+)]\\s*$")

        /** The callout a first line such as `[!note]` introduces, or null for any other text. */
        fun fromMarkerLine(line: String): CalloutType? {
            val name = MARKER.find(line.trim())?.groupValues?.get(1) ?: return null
            return entries.firstOrNull { it.marker.equals(name, ignoreCase = true) }
        }
    }
}

/** A blockquote that was recognised as a callout; its children are the quote's content. */
class CalloutBlock(val type: CalloutType) : CustomBlock()

/** Rewrites callout blockquotes in a parsed document into [CalloutBlock] nodes. */
object CalloutTransformer {

    fun apply(root: Node) {
        val found = ArrayList<Pair<BlockQuote, CalloutType>>()
        root.accept(object : AbstractVisitor() {
            override fun visit(blockQuote: BlockQuote) {
                detect(blockQuote)?.let { found += blockQuote to it }
                visitChildren(blockQuote)
            }
        })
        for ((quote, type) in found) replace(quote, type)
    }

    /** The paragraph nodes forming the marker line (and the line break after it), or null. */
    private fun markerNodes(quote: BlockQuote): Pair<CalloutType, List<Node>>? {
        val paragraph = quote.firstChild as? Paragraph ?: return null
        val nodes = ArrayList<Node>()
        val line = StringBuilder()
        var node = paragraph.firstChild
        while (node is Text) {
            line.append(node.literal)
            nodes += node
            node = node.next
        }
        if (node != null && (node is SoftLineBreak || node is HardLineBreak)) nodes += node
        else if (node != null) return null
        val type = CalloutType.fromMarkerLine(line.toString()) ?: return null
        return type to nodes
    }

    private fun detect(quote: BlockQuote): CalloutType? = markerNodes(quote)?.first

    private fun replace(quote: BlockQuote, type: CalloutType) {
        val (_, nodes) = markerNodes(quote) ?: return
        val paragraph = quote.firstChild
        nodes.forEach { it.unlink() }
        if (paragraph != null && paragraph.firstChild == null) paragraph.unlink()

        val callout = CalloutBlock(type)
        quote.insertBefore(callout)
        while (quote.firstChild != null) callout.appendChild(quote.firstChild)
        quote.unlink()
    }
}

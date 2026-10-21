package com.readmd.app.ui.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import com.readmd.app.domain.CalloutBlock
import com.readmd.app.domain.CalloutTransformer
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import org.commonmark.node.Node

/**
 * Renders GitHub-style alerts (`> [!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`,
 * `[!CAUTION]`) as a quote with a coloured bar and a bold coloured title line carrying
 * the type's icon. Ordinary blockquotes keep the default look.
 */
class CalloutPlugin(
    private val dark: Boolean,
    private val density: Float,
) : AbstractMarkwonPlugin() {

    override fun beforeRender(node: Node) {
        CalloutTransformer.apply(node)
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(CalloutBlock::class.java) { visitor, callout ->
            val color = callout.type.color(dark)
            visitor.blockStart(callout)
            val start = visitor.length()

            val title = "${callout.type.icon} ${callout.type.title}"
            val titleStart = visitor.length()
            visitor.builder().append(title)
            visitor.setSpans(titleStart, ForegroundColorSpan(color))
            visitor.setSpans(titleStart, StyleSpan(Typeface.BOLD))
            visitor.forceNewLine()

            visitor.visitChildren(callout)
            visitor.setSpans(start, CalloutBarSpan(color, density))
            visitor.blockEnd(callout)
        }
    }
}

/** Leading margin with a solid vertical bar in the callout's accent colour. */
private class CalloutBarSpan(
    private val color: Int,
    density: Float,
) : LeadingMarginSpan {

    private val barWidth = (BAR_DP * density).toInt()
    private val margin = (MARGIN_DP * density).toInt()

    override fun getLeadingMargin(first: Boolean): Int = margin

    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?,
    ) {
        val previousColor = paint.color
        val previousStyle = paint.style
        paint.color = color
        paint.style = Paint.Style.FILL
        val left = if (dir > 0) x else x - barWidth
        canvas.drawRect(left.toFloat(), top.toFloat(), (left + barWidth).toFloat(), bottom.toFloat(), paint)
        paint.color = previousColor
        paint.style = previousStyle
    }

    private companion object {
        const val BAR_DP = 4
        const val MARGIN_DP = 14
    }
}

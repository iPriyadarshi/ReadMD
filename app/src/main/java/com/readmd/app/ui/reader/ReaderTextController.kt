package com.readmd.app.ui.reader

import android.graphics.Typeface
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.util.TypedValue
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.core.spans.HeadingSpan
import com.readmd.app.domain.HeadingNode
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.ReaderFont

private class SearchSpan(color: Int) : BackgroundColorSpan(color)

/**
 * Bridges the rendered markdown [TextView] with Compose: tracks heading
 * scroll positions (for the table of contents) and applies search-result
 * highlight spans without re-rendering the document.
 *
 * All methods must be called on the main thread.
 */
class ReaderTextController {

    var textView: TextView? = null
        private set

    private var headings: List<HeadingNode> = emptyList()

    private var appliedScale = -1

    private var appliedFont: ReaderFont? = null

    private var appliedSpacing: LineSpacing? = null

    /** Scroll offset (px) for each heading index; null when unlocatable. */
    private var headingY: List<Float?> = emptyList()

    /** Invoked after every successful markdown render + layout pass. */
    var onRendered: (() -> Unit)? = null

    fun bind(tv: TextView) {
        if (textView !== tv) {
            textView = tv
            appliedScale = -1
            appliedFont = null
            appliedSpacing = null
            tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                refreshHeadingOffsets()
            }
        }
    }

    fun render(markwon: Markwon, markdown: String, headings: List<HeadingNode>, textColor: Int) {
        val tv = textView ?: return
        this.headings = headings
        markwon.setMarkdown(tv, markdown)
        tv.setTextColor(textColor)
        // Layout (and therefore heading positions) is available after the
        // next frame; notify listeners so search + TOC work immediately.
        tv.post {
            refreshHeadingOffsets()
            onRendered?.invoke()
        }
    }

    fun applyTextColor(textColor: Int) {
        textView?.setTextColor(textColor)
    }

    /** Sets the body font; inline code and code blocks keep their own monospace spans. */
    fun applyFont(font: ReaderFont) {
        val tv = textView ?: return
        if (font == appliedFont) return
        appliedFont = font
        tv.typeface = when (font) {
            ReaderFont.SANS -> Typeface.SANS_SERIF
            ReaderFont.SERIF -> Typeface.SERIF
            ReaderFont.MONO -> Typeface.MONOSPACE
        }
    }

    fun applyLineSpacing(spacing: LineSpacing) {
        val tv = textView ?: return
        if (spacing == appliedSpacing) return
        appliedSpacing = spacing
        tv.setLineSpacing(0f, spacing.multiplier)
    }

    /** Scales body text to [percent] of the default size; heading and code sizes follow. */
    fun applyTextScale(percent: Int) {
        val tv = textView ?: return
        if (percent == appliedScale) return
        appliedScale = percent
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, BASE_TEXT_SIZE_SP * percent / 100f)
    }

    /** Pixel y-offset of a heading inside the rendered text, or null. */
    fun offsetForHeading(index: Int): Float? {
        val cached = headingY.getOrNull(index)
        if (cached != null) return cached

        val tv = textView ?: return null
        val layout = tv.layout ?: return null
        val text = tv.text ?: return null
        val heading = headings.getOrNull(index) ?: return null

        val spanned = text as? Spanned
        if (spanned != null) {
            val spans = spanned.getSpans(0, spanned.length, HeadingSpan::class.java)
                .sortedBy { spanned.getSpanStart(it) }

            if (spans.size == headings.size && index in spans.indices) {
                val start = spanned.getSpanStart(spans[index])
                return layout.getLineTop(layout.getLineForOffset(start)).toFloat()
            }

            val target = heading.text.replace(WHITESPACE, " ").trim()
            for (span in spans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                val candidate = text.substring(start, end).replace(WHITESPACE, " ").trim()
                if (candidate.equals(target, ignoreCase = true) ||
                    candidate.startsWith(target.take(16), ignoreCase = true) ||
                    target.startsWith(candidate.take(16), ignoreCase = true)
                ) {
                    return layout.getLineTop(layout.getLineForOffset(start)).toFloat()
                }
            }
        }

        val target = heading.text.replace(WHITESPACE, " ").trim()
        val raw = text.toString()
        val pos = raw.indexOf(target, ignoreCase = true)
        if (pos >= 0) {
            return layout.getLineTop(layout.getLineForOffset(pos)).toFloat()
        }

        return null
    }

    fun applySearch(
        matches: List<IntRange>,
        currentIndex: Int,
        matchColor: Int,
        currentColor: Int,
    ) {
        val text = textView?.text as? Spannable ?: return
        clearSearchSpans(text)
        for ((i, range) in matches.withIndex()) {
            val start = range.first
            val end = range.last + 1
            if (start >= text.length || end > text.length) continue
            val span = if (i == currentIndex) SearchSpan(currentColor) else SearchSpan(matchColor)
            text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun clearSearch() {
        val text = textView?.text as? Spannable ?: return
        clearSearchSpans(text)
    }

    /** Pixel y-offset of a match inside the rendered text, or null. */
    fun pixelOffsetForMatch(matches: List<IntRange>, index: Int): Float? {
        val tv = textView ?: return null
        val layout = tv.layout ?: return null
        val range = matches.getOrNull(index) ?: return null
        if (range.first >= tv.text.length) return null
        return layout.getLineTop(layout.getLineForOffset(range.first)).toFloat()
    }

    private fun clearSearchSpans(text: Spannable) {
        text.getSpans(0, text.length, SearchSpan::class.java).forEach { text.removeSpan(it) }
    }

    /**
     * Maps our line-parsed [headings] onto the rendered [HeadingSpan]
     * instances (sorted by position) to obtain exact scroll positions.
     */
    private fun refreshHeadingOffsets() {
        val tv = textView ?: return
        val layout = tv.layout ?: return
        val text = tv.text as? Spanned ?: return
        val spans = text.getSpans(0, text.length, HeadingSpan::class.java)
            .sortedBy { text.getSpanStart(it) }
        val spanTexts = spans.map { span ->
            text.substring(text.getSpanStart(span), text.getSpanEnd(span))
                .trim()
                .replace(WHITESPACE, " ")
        }

        val result = ArrayList<Float?>(headings.size)
        var cursor = 0
        for ((i, heading) in headings.withIndex()) {
            var found = -1
            if (spans.size == headings.size && i in spans.indices) {
                found = i
            } else {
                val needle = heading.text.replace(WHITESPACE, " ").trim()
                for (j in cursor until spanTexts.size) {
                    val candidate = spanTexts[j]
                    if (candidate.equals(needle, ignoreCase = true) ||
                        candidate.startsWith(needle.take(16), ignoreCase = true) ||
                        needle.startsWith(candidate.take(16), ignoreCase = true)
                    ) {
                        found = j
                        break
                    }
                }
            }

            if (found >= 0) {
                cursor = found + 1
                val spanStart = text.getSpanStart(spans[found])
                result += layout.getLineTop(layout.getLineForOffset(spanStart)).toFloat()
            } else {
                val needle = heading.text.replace(WHITESPACE, " ").trim()
                val pos = text.toString().indexOf(needle, ignoreCase = true)
                if (pos >= 0) {
                    result += layout.getLineTop(layout.getLineForOffset(pos)).toFloat()
                } else {
                    result += null
                }
            }
        }
        headingY = result
    }

    private companion object {
        const val BASE_TEXT_SIZE_SP = 14f
        val WHITESPACE = Regex("\\s+")
    }
}

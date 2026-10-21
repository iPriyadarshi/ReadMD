package com.readmd.app.ui.reader

import android.text.Spannable
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.TextPaint
import android.view.View
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.image.AsyncDrawableSpan

/**
 * Makes every image in the rendered text tappable. Images that are already inside a link
 * keep following that link, so only plain images get the viewer.
 */
class ImageClickPlugin(private val onImageClick: (String) -> Unit) : AbstractMarkwonPlugin() {

    override fun afterSetText(textView: TextView) {
        val text = textView.text as? Spannable ?: return
        for (image in text.getSpans(0, text.length, AsyncDrawableSpan::class.java)) {
            val start = text.getSpanStart(image)
            val end = text.getSpanEnd(image)
            if (start < 0 || end <= start) continue
            if (text.getSpans(start, end, URLSpan::class.java).isNotEmpty()) continue

            val source = image.drawable.destination
            text.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) = onImageClick(source)

                    // Images are not text, so no link colour or underline.
                    override fun updateDrawState(ds: TextPaint) = Unit
                },
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
}

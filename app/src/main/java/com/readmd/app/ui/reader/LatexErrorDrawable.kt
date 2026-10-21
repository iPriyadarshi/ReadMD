package com.readmd.app.ui.reader

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable

/**
 * Shown in place of a formula that can't be parsed: draws the original LaTeX source
 * as plain monospace text so nothing disappears from the document.
 */
class LatexErrorDrawable(
    private val source: String,
    textSizePx: Float,
    color: Int,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = textSizePx
        typeface = Typeface.MONOSPACE
    }

    private val width = paint.measureText(source).toInt() + 1
    private val height = (paint.descent() - paint.ascent()).toInt() + 1

    init {
        setBounds(0, 0, width, height)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawText(source, bounds.left.toFloat(), bounds.top - paint.ascent(), paint)
    }

    override fun getIntrinsicWidth(): Int = width

    override fun getIntrinsicHeight(): Int = height

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

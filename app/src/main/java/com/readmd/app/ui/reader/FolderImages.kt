package com.readmd.app.ui.reader

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.DocumentsContract
import com.readmd.app.data.DocumentRepository
import com.readmd.app.domain.RelativeLink
import io.noties.markwon.image.destination.ImageDestinationProcessor

/**
 * Turns image paths written relative to the document (`images/a.png`, `../b.png`) into
 * content URIs inside the folder the document was opened from, so they load through the
 * ContentResolver. Anything else, and documents not opened from a folder, pass through.
 */
class FolderImageDestinationProcessor(docId: String) : ImageDestinationProcessor() {

    private val documentUri: Uri? =
        if (docId.startsWith(DocumentRepository.BUNDLED_PREFIX)) null else Uri.parse(docId)

    override fun process(destination: String): String {
        val uri = documentUri ?: return destination
        if (!RelativeLink.isRelativeFileLink(destination)) return destination
        if (!DocumentsContract.isTreeUri(uri)) return destination
        val target = RelativeLink.resolve(DocumentsContract.getDocumentId(uri), destination)
            ?: return destination
        return DocumentsContract.buildDocumentUriUsingTree(uri, target).toString()
    }
}

/** Boxed text shown where an image could not be loaded, so the gap is visible and named. */
class ImagePlaceholderDrawable(
    private val label: String,
    textSizePx: Float,
    color: Int,
) : Drawable() {

    private val padding = textSizePx * 0.6f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = textSizePx
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = textSizePx / 12f
        alpha = 140
    }

    private val width = (textPaint.measureText(label) + padding * 2).toInt()
    private val height = (textPaint.descent() - textPaint.ascent() + padding * 2).toInt()

    init {
        setBounds(0, 0, width, height)
    }

    override fun draw(canvas: Canvas) {
        val box = RectF(bounds)
        box.inset(borderPaint.strokeWidth, borderPaint.strokeWidth)
        canvas.drawRoundRect(box, padding / 2, padding / 2, borderPaint)
        canvas.drawText(label, bounds.left + padding, bounds.top + padding - textPaint.ascent(), textPaint)
    }

    override fun getIntrinsicWidth(): Int = width

    override fun getIntrinsicHeight(): Int = height

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

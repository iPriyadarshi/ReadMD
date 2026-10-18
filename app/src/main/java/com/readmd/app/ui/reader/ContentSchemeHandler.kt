package com.readmd.app.ui.reader

import android.content.Context
import android.net.Uri
import io.noties.markwon.image.ImageItem
import io.noties.markwon.image.SchemeHandler

/**
 * Lets markdown images that reference content:// URIs (the normal case for
 * files picked via SAF, including images sitting next to the .md file in a
 * picked folder) resolve through the ContentResolver.
 *
 * [handle] runs on Markwon's background image executor.
 */
class ContentSchemeHandler(private val context: Context) : SchemeHandler() {

    override fun handle(raw: String, uri: Uri): ImageItem {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw java.io.IOException("Cannot open image: $raw")
        val type = context.contentResolver.getType(uri)
        return ImageItem.withDecodingNeeded(type, stream)
    }

    override fun supportedSchemes(): Collection<String> = listOf(SCHEME)

    companion object {
        const val SCHEME = "content"
    }
}

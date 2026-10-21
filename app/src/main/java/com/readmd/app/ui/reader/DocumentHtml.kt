package com.readmd.app.ui.reader

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.readmd.app.domain.HtmlExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Builds the standalone HTML page for a document, shared by printing and by HTML export. */
class DocumentHtml(private val context: Context) {

    /**
     * Renders [markdown] to a complete HTML page. Images that sit in the document's folder
     * are embedded as data URIs so the page works on its own, wherever it is opened.
     */
    suspend fun build(docId: String, title: String, markdown: String): String =
        withContext(Dispatchers.IO) {
            HtmlExport.render(title, markdown) { source -> embeddedImage(docId, source) }
        }

    /** Writes [html] to the document the user picked, replacing its contents. */
    suspend fun save(destination: Uri, html: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("Cannot open the destination")
            stream.use { it.write(html.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
    }

    private fun embeddedImage(docId: String, source: String): String? {
        val address = FolderImageDestinationProcessor(docId).process(source)
        if (address == source) return null
        val uri = Uri.parse(address)
        if (uri.scheme != "content") return null
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readLimited(MAX_IMAGE_BYTES) }
                ?: return null
            val type = context.contentResolver.getType(uri) ?: "image/*"
            "data:$type;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > limit) throw IOException("Image too large to embed")
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
    }
}

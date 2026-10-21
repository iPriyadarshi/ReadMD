package com.readmd.app.ui.reader

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.readmd.app.domain.HtmlExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hands a document to the system print service as paginated pages. The print dialog offers
 * "Save as PDF", where the user chooses the destination, as well as real printers.
 *
 * Keep one instance per screen and call [release] when the screen goes away; a pending
 * print job keeps using the page it was given.
 */
class DocumentPrinter(private val context: Context) {

    private var webView: WebView? = null

    /** Renders [markdown] to HTML (embedding images found next to the document) and opens the print dialog. */
    suspend fun print(docId: String, title: String, markdown: String) {
        val html = withContext(Dispatchers.IO) {
            HtmlExport.render(title, markdown) { source -> embeddedImage(docId, source) }
        }

        release()
        val view = WebView(context)
        view.settings.javaScriptEnabled = false
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(page: WebView, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build()
                manager.print(title, page.createPrintDocumentAdapter(title), attributes)
            }
        }
        webView = view
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun release() {
        webView?.destroy()
        webView = null
    }

    /**
     * Reads an image that sits in the document's folder and returns it as a data URI so the
     * print page needs no access to the folder. Web images and unreadable files return null.
     */
    private fun embeddedImage(docId: String, source: String): String? {
        val address = FolderImageDestinationProcessor(docId).process(source)
        if (address == source) return null
        val uri = Uri.parse(address)
        if (uri.scheme != "content") return null
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes(MAX_IMAGE_BYTES) }
                ?: return null
            val type = context.contentResolver.getType(uri) ?: "image/*"
            "data:$type;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun java.io.InputStream.readBytes(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > limit) throw java.io.IOException("Image too large to embed")
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
    }
}

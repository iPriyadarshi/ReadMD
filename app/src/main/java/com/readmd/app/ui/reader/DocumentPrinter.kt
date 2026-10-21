package com.readmd.app.ui.reader

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Hands an HTML page to the system print service as paginated pages. The print dialog offers
 * "Save as PDF", where the user chooses the destination, as well as real printers.
 *
 * Keep one instance per screen and call [release] when the screen goes away; a pending
 * print job keeps using the page it was given.
 */
class DocumentPrinter(private val context: Context) {

    private var webView: WebView? = null

    fun print(title: String, html: String) {
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
}

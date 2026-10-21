package com.readmd.app.ui.reader

import com.readmd.app.R
import androidx.compose.ui.res.stringResource
import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.readmd.app.domain.MermaidBlocks

/**
 * Full-screen viewer that renders a mermaid diagram with the bundled mermaid script.
 * Pinch to zoom is handled by the WebView. If rendering fails the page shows the
 * error, and the original code block remains in the document.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidDialog(source: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val background = colors.background.toCssHex()
    val foreground = colors.onBackground.toCssHex()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.diagram_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_diagram))
                        }
                    },
                )
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(colors.background.toArgb())
                            settings.javaScriptEnabled = true
                            // The page only needs the bundled script; keep everything else out.
                            settings.blockNetworkLoads = true
                            settings.allowContentAccess = false
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            loadDataWithBaseURL(
                                "file:///android_asset/mermaid/",
                                MermaidBlocks.html(source, dark, background, foreground),
                                "text/html",
                                "UTF-8",
                                null,
                            )
                        }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

private fun Color.toCssHex(): String = String.format("#%06X", toArgb() and 0xFFFFFF)

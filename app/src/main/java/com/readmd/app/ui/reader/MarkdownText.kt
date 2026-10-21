package com.readmd.app.ui.reader

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.readmd.app.domain.HeadingNode
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.ReaderFont

/**
 * Renders markdown into a single TextView via Markwon (full markdown
 * fidelity) hosted inside the Compose tree.
 */
@Composable
fun MarkdownText(
    markdown: String,
    headings: List<HeadingNode>,
    controller: ReaderTextController,
    contentColor: Color,
    textScale: Int,
    font: ReaderFont,
    lineSpacing: LineSpacing,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Follow the app theme (not the raw system setting) so code-block
    // highlighting and backgrounds match light/dark mode.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val markwon = remember(dark) { buildMarkwon(context, dark) { currentOnLinkClick(it) } }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextIsSelectable(true)
                controller.bind(this)
            }
        },
        update = { tv -> controller.bind(tv) },
        modifier = modifier.fillMaxWidth(),
    )

    LaunchedEffect(markdown, headings) {
        controller.render(markwon, markdown, headings, contentColor.toArgb())
    }

    // Keep text color and size in sync with theme/setting changes without re-rendering.
    SideEffect {
        controller.applyTextColor(contentColor.toArgb())
        controller.applyTextScale(textScale)
        controller.applyFont(font)
        controller.applyLineSpacing(lineSpacing)
    }
}

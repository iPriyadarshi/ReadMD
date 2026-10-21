package com.readmd.app.ui.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Plain-text editor for a document's markdown source, in the same monospace style as the raw view. */
@Composable
fun MarkdownEditor(
    text: String,
    onTextChange: (String) -> Unit,
    textScale: Int,
    horizontalPadding: Int,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.typography.bodySmall
    val style = base.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = base.fontSize * textScale / 100f,
        lineHeight = base.lineHeight * textScale / 100f,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding.dp, vertical = 16.dp),
    )
}

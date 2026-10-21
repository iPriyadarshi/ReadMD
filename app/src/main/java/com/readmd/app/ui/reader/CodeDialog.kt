package com.readmd.app.ui.reader

import com.readmd.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen view of one code block. Lines are never wrapped; the code scrolls sideways
 * next to a fixed column of line numbers, and the text can be selected or copied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeDialog(code: String, onCopy: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    val lineNumbers = (1..code.lines().size).joinToString("\n")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.code_dialog_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_code))
                        }
                    },
                    actions = {
                        IconButton(onClick = onCopy) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_copy_code))
                        }
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = lineNumbers,
                        style = style,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            text = code,
                            style = style,
                            softWrap = false,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

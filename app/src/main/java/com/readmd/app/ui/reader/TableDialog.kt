package com.readmd.app.ui.reader

import com.readmd.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.readmd.app.domain.ColumnAlignment
import com.readmd.app.domain.TableData

/**
 * Full-screen view of a wide table at its natural size. It scrolls sideways (and down)
 * independently of the page, so columns keep their width instead of being squeezed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDialog(table: TableData, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val headerStyle = bodyStyle.copy(fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellPadding = 12.dp

    // Each column is as wide as its widest cell (header in bold), plus padding.
    val columnWidths: List<Dp> = remember(table, bodyStyle) {
        table.header.indices.map { column ->
            val widest = buildList {
                add(measurer.measure(table.header[column], headerStyle).size.width)
                table.rows.forEach { add(measurer.measure(it.getOrElse(column) { "" }, bodyStyle).size.width) }
            }.max()
            with(density) { widest.toDp() } + cellPadding * 2
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.table_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_table))
                        }
                    },
                )
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        TableRow(
                            cells = table.header,
                            widths = columnWidths,
                            alignments = table.alignments,
                            style = headerStyle,
                            background = colors.surfaceVariant,
                            cellPadding = cellPadding,
                        )
                        table.rows.forEachIndexed { index, row ->
                            TableRow(
                                cells = row,
                                widths = columnWidths,
                                alignments = table.alignments,
                                style = bodyStyle,
                                background = if (index % 2 == 1) colors.surfaceContainerLow else colors.background,
                                cellPadding = cellPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    widths: List<Dp>,
    alignments: List<ColumnAlignment>,
    style: androidx.compose.ui.text.TextStyle,
    background: androidx.compose.ui.graphics.Color,
    cellPadding: Dp,
) {
    val border = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.background(background)) {
        widths.forEachIndexed { column, width ->
            Text(
                text = cells.getOrElse(column) { "" },
                style = style,
                softWrap = false,
                textAlign = when (alignments.getOrElse(column) { ColumnAlignment.START }) {
                    ColumnAlignment.START -> TextAlign.Start
                    ColumnAlignment.CENTER -> TextAlign.Center
                    ColumnAlignment.END -> TextAlign.End
                },
                modifier = Modifier
                    .width(width)
                    .border(0.5.dp, border)
                    .padding(horizontal = cellPadding, vertical = 8.dp),
            )
        }
    }
}

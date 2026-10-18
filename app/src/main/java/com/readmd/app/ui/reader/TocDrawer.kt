package com.readmd.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readmd.app.domain.HeadingNode

@Composable
fun TocDrawer(
    headings: List<HeadingNode>,
    onSelect: (Int) -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "Contents",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
        )
        HorizontalDivider()
        if (headings.isEmpty()) {
            Text(
                text = "This document has no headings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn {
                itemsIndexed(headings) { index, heading ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = heading.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        leadingContent = {
                            Text(
                                text = "H${heading.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier
                            .padding(start = (heading.level * 10).dp)
                            .clickable { onSelect(index) },
                    )
                }
            }
        }
    }
}

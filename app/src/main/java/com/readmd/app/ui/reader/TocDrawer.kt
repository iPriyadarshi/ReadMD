package com.readmd.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readmd.app.domain.Bookmark
import com.readmd.app.domain.HeadingNode

@Composable
fun TocDrawer(
    headings: List<HeadingNode>,
    bookmarks: List<Bookmark>,
    onSelect: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    onSelectBookmark: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit,
) {
    ModalDrawerSheet {
        LazyColumn {
            item(key = "contents-title") {
                Text(
                    text = "Contents",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                )
                HorizontalDivider()
            }
            if (headings.isEmpty()) {
                item(key = "contents-empty") {
                    Text(
                        text = "This document has no headings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                itemsIndexed(headings, key = { index, _ -> "heading-$index" }) { index, heading ->
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

            item(key = "bookmarks-title") {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 8.dp),
                ) {
                    Text(
                        text = "Bookmarks",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onAddBookmark) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Bookmark current position")
                    }
                }
            }
            if (bookmarks.isEmpty()) {
                item(key = "bookmarks-empty") {
                    Text(
                        text = "Tap the bookmark button to save your place",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            } else {
                items(bookmarks, key = { "bookmark-${it.permille}" }) { bookmark ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = bookmark.label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        supportingContent = { Text("${bookmark.permille / 10}% through") },
                        trailingContent = {
                            IconButton(onClick = { onRemoveBookmark(bookmark) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove bookmark")
                            }
                        },
                        modifier = Modifier.clickable { onSelectBookmark(bookmark) },
                    )
                }
            }
        }
    }
}

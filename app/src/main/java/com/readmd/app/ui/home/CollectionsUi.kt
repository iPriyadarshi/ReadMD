package com.readmd.app.ui.home

import com.readmd.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.readmd.app.domain.Collections
import com.readmd.app.domain.DocCollection

/** "All" plus one chip per collection; long-press a collection chip to rename or delete it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionChips(
    collections: List<DocCollection>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onRename: (DocCollection) -> Unit,
    onDelete: (DocCollection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            ChipBox(label = stringResource(R.string.chip_all), selected = selectedId == null, onClick = { onSelect(null) })
        }
        items(collections, key = { it.id }) { collection ->
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                ChipBox(
                    label = collection.name,
                    selected = selectedId == collection.id,
                    onClick = { onSelect(collection.id) },
                    onLongClick = { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = { menuOpen = false; onRename(collection) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = { menuOpen = false; onDelete(collection) },
                    )
                }
            }
        }
        item(key = "new") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = onCreate)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.new_collection), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChipBox(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.onSecondaryContainer else colors.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.secondaryContainer else colors.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Text prompt used for both creating and renaming a collection. */
@Composable
fun CollectionNameDialog(
    title: String,
    confirmLabel: String,
    initial: String,
    collections: List<DocCollection>,
    exceptId: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val name = Collections.normalizeName(text)
    val error = when {
        text.isEmpty() -> null
        name.isEmpty() -> stringResource(R.string.collection_name_required)
        Collections.nameTaken(collections, name, exceptId) -> stringResource(R.string.collection_name_taken)
        else -> null
    }
    val valid = name.isNotEmpty() && error == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.label_name)) },
                isError = error != null,
                supportingText = { if (error != null) Text(error) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun DeleteCollectionDialog(
    collection: DocCollection,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_collection_title, collection.name)) },
        text = { Text(stringResource(R.string.delete_collection_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Lets the user tick which collections a document belongs to, or start a new one for it. */
@Composable
fun AssignCollectionsDialog(
    docName: String,
    collections: List<DocCollection>,
    memberOf: Set<String>,
    onToggle: (collection: DocCollection, member: Boolean) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.assign_collections_title), maxLines = 1) },
        text = {
            Column {
                Text(
                    text = docName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (collections.isEmpty()) {
                    Text(stringResource(R.string.no_collections), style = MaterialTheme.typography.bodyMedium)
                }
                collections.forEach { collection ->
                    val member = collection.id in memberOf
                    ListItem(
                        headlineContent = { Text(collection.name, maxLines = 1) },
                        leadingContent = {
                            Checkbox(checked = member, onCheckedChange = null)
                        },
                        modifier = Modifier.clickable { onToggle(collection, !member) },
                    )
                }
                TextButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.new_collection))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

package com.readmd.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import com.readmd.app.ui.theme.AccentColor
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readmd.app.data.FolderSort
import com.readmd.app.ui.theme.ThemeMode

private val markdownMimeTypes = arrayOf(
    "text/markdown",
    "text/x-markdown",
    "text/plain",
    "application/octet-stream",
    "text/*",
    "*/*",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    /** Opens a document; [query] is non-null when opened from a folder search result. */
    onOpenDoc: (id: String, query: String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val accent by viewModel.accent.collectAsState()
    val search by viewModel.search.collectAsState()
    var folderSearchOpen by remember { mutableStateOf(false) }
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.mapTo(HashSet()) { it.id } }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFilePicked(uri)
            onOpenDoc(uri.toString(), null)
        }
    }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.onFolderPicked(uri)
    }

    var themeMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ReadMD") },
                actions = {
                    IconButton(onClick = { themeMenuOpen = true }) {
                        Icon(
                            imageVector = when (state.themeMode) {
                                ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SEPIA -> Icons.Default.AutoStories
                                ThemeMode.BLACK -> Icons.Default.Contrast
                            },
                            contentDescription = "Theme",
                        )
                    }
                    DropdownMenu(
                        expanded = themeMenuOpen,
                        onDismissRequest = { themeMenuOpen = false },
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    themeMenuOpen = false
                                },
                            )
                        }
                        HorizontalDivider()
                        Text(
                            text = "Accent color",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        AccentColor.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setAccent(option)
                                    themeMenuOpen = false
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(option.swatch(darkSurface)),
                                    )
                                },
                                trailingIcon = {
                                    if (option == accent) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected")
                                    }
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { filePicker.launch(markdownMimeTypes) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open file")
                        }
                        OutlinedButton(
                            onClick = { folderPicker.launch(null) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open folder")
                        }
                    }
                }
            }

            // Folder section
            state.folder?.let { folder ->
                item(key = "folder-header") {
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = folder.label,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    folderSearchOpen = !folderSearchOpen
                                    if (!folderSearchOpen) viewModel.clearSearch()
                                },
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search in folder")
                            }
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Sort files")
                            }
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false },
                            ) {
                                FolderSort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(sort.label) },
                                        onClick = {
                                            viewModel.setFolderSort(sort)
                                            sortMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                        if (!folder.scanning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = folder.entries.size.let {
                                        if (it == 1) "1 file" else "$it files"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (folder.subfoldersScanned > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ) {
                                        Text(
                                            if (folder.subfoldersScanned == 1) {
                                                "+1 subfolder"
                                            } else {
                                                "+${folder.subfoldersScanned} subfolders"
                                            },
                                        )
                                    }
                                }
                                if (folder.truncated) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "showing first ${folder.entries.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (folder.scanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        folder.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                val showingResults = folderSearchOpen && search.active
                if (folderSearchOpen) {
                    item(key = "folder-search-field") {
                        OutlinedTextField(
                            value = search.query,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text("Search file contents") },
                            singleLine = true,
                            trailingIcon = {
                                if (search.query.isNotEmpty()) {
                                    IconButton(onClick = viewModel::clearSearch) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        if (search.searching) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
                if (showingResults) {
                    if (!search.searching && search.hits.isEmpty()) {
                        item(key = "folder-search-empty") {
                            Text(
                                "No matches",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(search.hits, key = { "hit-${it.id}" }) { hit ->
                        ListItem(
                            headlineContent = { Text(hit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(hit.match.snippet, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                            trailingContent = {
                                Text(
                                    text = hit.match.count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.clickable { onOpenDoc(hit.id, search.query.trim()) },
                        )
                    }
                } else {
                    if (!folder.scanning && folder.entries.isEmpty()) {
                        item {
                            Text(
                                "No markdown files in this folder",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(folder.entries, key = { "folder-${it.id}" }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.clickable { onOpenDoc(entry.id, null) },
                        )
                    }
                }
            }

            // Favorites
            if (favorites.isNotEmpty()) {
                item(key = "favorites-header") {
                    Text(
                        "Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                    HorizontalDivider()
                }
                items(favorites, key = { "favorite-${it.id}" }) { favorite ->
                    ListItem(
                        headlineContent = { Text(favorite.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeFavorite(favorite.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from favorites")
                            }
                        },
                        modifier = Modifier.clickable { onOpenDoc(favorite.id, null) },
                    )
                }
            }

            // Recents
            item(key = "recents-header") {
                Text(
                    "Recent files",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
                HorizontalDivider()
            }
            if (state.recents.isEmpty()) {
                item(key = "recents-empty") {
                    Text(
                        "Files you read will show up here",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.recents, key = { "recent-${it.id}" }) { recent ->
                ListItem(
                    headlineContent = { Text(recent.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isFavorite = recent.id in favoriteIds
                            IconButton(
                                onClick = {
                                    if (isFavorite) {
                                        viewModel.removeFavorite(recent.id)
                                    } else {
                                        viewModel.addFavorite(recent.id, recent.name)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                )
                            }
                            IconButton(onClick = { viewModel.removeRecent(recent.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from recents")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onOpenDoc(recent.id, null) },
                )
            }

            // Samples
            item(key = "samples-header") {
                Text(
                    "Sample documents",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
                HorizontalDivider()
            }
            items(viewModel.samples, key = { it.id }) { sample ->
                ListItem(
                    headlineContent = { Text(sample.name) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    modifier = Modifier.clickable { onOpenDoc(sample.id, null) },
                )
            }
        }
    }
}

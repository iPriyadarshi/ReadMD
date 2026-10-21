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
import com.readmd.app.domain.DocCollection
import com.readmd.app.domain.Collections
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.readmd.app.domain.NameFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.BackHandler
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    /** Opens a document; [query] is non-null when opened from a folder search result. */
    onOpenDoc: (id: String, query: String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val accent by viewModel.accent.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val collectionItems by viewModel.collectionItems.collectAsState()
    val selectedCollectionId by viewModel.selectedCollection.collectAsState()
    val selectedCollection = collections.firstOrNull { it.id == selectedCollectionId }
    var assignTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var createCollectionFor by remember { mutableStateOf<Pair<String, String>?>(null) }
    var createCollectionOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DocCollection?>(null) }
    var deleteTarget by remember { mutableStateOf<DocCollection?>(null) }
    val search by viewModel.search.collectAsState()
    var folderSearchOpen by remember { mutableStateOf(false) }
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.mapTo(HashSet()) { it.id } }

    val filePicker = rememberLauncherForActivityResult(
        OpenEditableDocument(),
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

    // Live filter of the favorites, recents and folder lists by file name.
    var nameFilterActive by rememberSaveable { mutableStateOf(false) }
    var nameQuery by rememberSaveable { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    val shownFavorites = remember(favorites, nameQuery) { NameFilter.filter(favorites, nameQuery) { it.name } }
    val shownRecents = remember(state.recents, nameQuery) { NameFilter.filter(state.recents, nameQuery) { it.name } }
    LaunchedEffect(nameFilterActive) {
        if (nameFilterActive) nameFocus.requestFocus()
    }
    BackHandler(enabled = nameFilterActive) {
        nameFilterActive = false
        nameQuery = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (nameFilterActive) {
                        TextField(
                            value = nameQuery,
                            onValueChange = { nameQuery = it },
                            placeholder = { Text("Filter by file name") },
                            singleLine = true,
                            trailingIcon = {
                                if (nameQuery.isNotEmpty()) {
                                    IconButton(onClick = { nameQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear filter")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocus),
                        )
                    } else {
                        Text("ReadMD")
                    }
                },
                navigationIcon = {
                    if (nameFilterActive) {
                        IconButton(onClick = { nameFilterActive = false; nameQuery = "" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close filter")
                        }
                    }
                },
                actions = {
                    if (!nameFilterActive) {
                        IconButton(onClick = { nameFilterActive = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter files by name")
                        }
                    }
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

            item(key = "collection-chips") {
                CollectionChips(
                    collections = collections,
                    selectedId = selectedCollection?.id,
                    onSelect = viewModel::selectCollection,
                    onCreate = { createCollectionOpen = true },
                    onRename = { renameTarget = it },
                    onDelete = { deleteTarget = it },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (selectedCollection != null) {
                val docs = NameFilter.filter(
                    Collections.docsIn(collectionItems, selectedCollection.id),
                    nameQuery,
                ) { it.name }
                item(key = "collection-header") {
                    Text(
                        selectedCollection.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                    HorizontalDivider()
                }
                if (docs.isEmpty()) {
                    item(key = "collection-empty") {
                        Text(
                            if (nameQuery.isBlank()) {
                                "No files here yet. Long-press a file in your recents or folder to add it."
                            } else {
                                "No files match"
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(docs, key = { "collection-doc-${it.docId}" }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenDoc(item.docId, null) },
                            onLongClick = { assignTarget = item.docId to item.name },
                        ),
                    )
                }
            } else {
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
                    val shownEntries = NameFilter.filter(folder.entries, nameQuery) { it.name }
                    if (!folder.scanning && shownEntries.isEmpty()) {
                        item {
                            Text(
                                if (folder.entries.isEmpty()) "No markdown files in this folder" else "No files match",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(shownEntries, key = { "folder-${it.id}" }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.combinedClickable(
                                onClick = { onOpenDoc(entry.id, null) },
                                onLongClick = { assignTarget = entry.id to entry.name },
                            ),
                        )
                    }
                }
            }

            // Favorites
            if (shownFavorites.isNotEmpty()) {
                item(key = "favorites-header") {
                    Text(
                        "Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                    HorizontalDivider()
                }
                items(shownFavorites, key = { "favorite-${it.id}" }) { favorite ->
                    ListItem(
                        headlineContent = { Text(favorite.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeFavorite(favorite.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from favorites")
                            }
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenDoc(favorite.id, null) },
                            onLongClick = { assignTarget = favorite.id to favorite.name },
                        ),
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
            if (shownRecents.isEmpty()) {
                item(key = "recents-empty") {
                    Text(
                        if (state.recents.isEmpty()) "Files you read will show up here" else "No recent files match",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(shownRecents, key = { "recent-${it.id}" }) { recent ->
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
                    modifier = Modifier.combinedClickable(
                        onClick = { onOpenDoc(recent.id, null) },
                        onLongClick = { assignTarget = recent.id to recent.name },
                    ),
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

    // Collection dialogs
    if (createCollectionOpen || createCollectionFor != null) {
        val forDoc = createCollectionFor
        CollectionNameDialog(
            title = "New collection",
            confirmLabel = "Create",
            initial = "",
            collections = collections,
            exceptId = null,
            onConfirm = { name ->
                viewModel.createCollection(name, forDoc?.first, forDoc?.second)
                createCollectionOpen = false
                createCollectionFor = null
            },
            onDismiss = {
                createCollectionOpen = false
                createCollectionFor = null
            },
        )
    }
    renameTarget?.let { target ->
        CollectionNameDialog(
            title = "Rename collection",
            confirmLabel = "Rename",
            initial = target.name,
            collections = collections,
            exceptId = target.id,
            onConfirm = { name ->
                viewModel.renameCollection(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        DeleteCollectionDialog(
            collection = target,
            onConfirm = {
                viewModel.deleteCollection(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
    assignTarget?.let { (docId, docName) ->
        AssignCollectionsDialog(
            docName = docName,
            collections = collections,
            memberOf = Collections.collectionIdsOf(collectionItems, docId),
            onToggle = { collection, member ->
                viewModel.setInCollection(collection.id, docId, docName, member)
            },
            onCreate = {
                createCollectionFor = docId to docName
                assignTarget = null
            },
            onDismiss = { assignTarget = null },
        )
    }
}

package com.readmd.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.readmd.app.R
import com.readmd.app.ReadMdApp
import com.readmd.app.data.FolderEntry
import com.readmd.app.data.FolderSort
import com.readmd.app.data.SearchHit
import com.readmd.app.data.recent.RecentEntry
import com.readmd.app.di.AppContainer
import com.readmd.app.domain.TextSearch
import com.readmd.app.ui.theme.AccentColor
import com.readmd.app.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SampleDoc(
    val id: String,
    val name: String,
)

data class FolderUiState(
    val treeUri: String,
    val label: String,
    val scanning: Boolean = false,
    /** Raw (unsorted) scan results; [FolderSort] is applied on read. */
    val entries: List<FolderEntry> = emptyList(),
    /** How many subfolders the recursive scan descended into. */
    val subfoldersScanned: Int = 0,
    /** True when the scan hit its file cap. */
    val truncated: Boolean = false,
    val error: String? = null,
)

data class HomeUiState(
    val recents: List<RecentEntry> = emptyList(),
    val folder: FolderUiState? = null,
    val folderSort: FolderSort = FolderSort.NAME_ASC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/** State of a content search inside the picked folder. */
data class FolderSearchState(
    val query: String = "",
    val searching: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
) {
    val active: Boolean get() = query.length >= TextSearch.MIN_QUERY_LENGTH
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _search = MutableStateFlow(FolderSearchState())
    val search = _search.asStateFlow()
    private var searchJob: Job? = null

    /** Debounced, cancellable content search over the scanned folder entries. */
    fun setSearchQuery(query: String) {
        searchJob?.cancel()
        _search.value = FolderSearchState(query = query)
        if (query.trim().length < TextSearch.MIN_QUERY_LENGTH) return
        val entries = folderState.value?.entries.orEmpty()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _search.update { it.copy(searching = true) }
            container.folderSearcher.search(entries, query.trim())
                .onCompletion { _search.update { it.copy(searching = false) } }
                .collect { hit -> _search.update { it.copy(hits = it.hits + hit) } }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = FolderSearchState()
    }

    private val folderState = MutableStateFlow<FolderUiState?>(null)

    val uiState = combine(
        container.recentFiles.recents,
        container.recentFiles.folderTree,
        container.recentFiles.themeMode,
        container.recentFiles.folderSort,
        folderState,
    ) { recents, folderUri, themeOrdinal, sortId, scanned ->
        val sort = FolderSort.fromId(sortId)
        // When the folder is un-picked, treeUri is null and the stale
        // scan result is dropped (a non-null scan never matches null).
        val scannedFolder = scanned?.takeIf { it.treeUri == folderUri }
        HomeUiState(
            recents = recents,
            folder = (scannedFolder
                ?: folderUri?.let {
                    FolderUiState(treeUri = it, label = container.appContext.getString(R.string.selected_folder), scanning = true)
                })?.copy(entries = sort.apply(scannedFolder?.entries.orEmpty())),
            folderSort = sort,
            themeMode = ThemeMode.fromOrdinal(themeOrdinal),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val samples = listOf(
        SampleDoc("res:welcome", container.appContext.getString(R.string.sample_welcome)),
        SampleDoc("res:cheatsheet", container.appContext.getString(R.string.sample_cheatsheet)),
    )

    init {
        viewModelScope.launch {
            container.recentFiles.folderTree.collect { uri ->
                if (uri == null) {
                    folderState.value = null
                } else {
                    rescanFolder(Uri.parse(uri))
                }
            }
        }
    }

    /** Called after the file picker returns; take persistable permission so recents remain accessible. */
    fun onFilePicked(uri: Uri) {
        container.documents.takePersistablePermission(uri)
    }

    fun onFolderPicked(uri: Uri) {
        viewModelScope.launch {
            container.documents.takePersistablePermission(uri)
            container.recentFiles.setFolderTree(uri.toString())
            // The folderTree flow collector triggers the scan.
        }
    }

    fun setFolderSort(sort: FolderSort) {
        viewModelScope.launch { container.recentFiles.setFolderSort(sort.id) }
    }

    fun removeRecent(id: String) {
        viewModelScope.launch { container.recentFiles.remove(id) }
    }

    val collections = container.recentFiles.collections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectionItems = container.recentFiles.collectionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The collection the home screen is filtered to, or null for everything. */
    private val _selectedCollection = MutableStateFlow<String?>(null)
    val selectedCollection = _selectedCollection.asStateFlow()

    fun selectCollection(id: String?) {
        _selectedCollection.value = id
    }

    /** Creates a collection; when [docId] is given the document is added to it right away. */
    fun createCollection(name: String, docId: String? = null, docName: String? = null) {
        viewModelScope.launch {
            val id = container.recentFiles.createCollection(name)
            if (docId != null && docName != null) container.recentFiles.addToCollection(id, docId, docName)
        }
    }

    fun renameCollection(id: String, name: String) {
        viewModelScope.launch { container.recentFiles.renameCollection(id, name) }
    }

    fun deleteCollection(id: String) {
        if (_selectedCollection.value == id) _selectedCollection.value = null
        viewModelScope.launch { container.recentFiles.deleteCollection(id) }
    }

    fun setInCollection(collectionId: String, docId: String, docName: String, member: Boolean) {
        viewModelScope.launch {
            if (member) {
                container.recentFiles.addToCollection(collectionId, docId, docName)
            } else {
                container.recentFiles.removeFromCollection(collectionId, docId)
            }
        }
    }

    val favorites = container.recentFiles.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addFavorite(id: String, name: String) {
        viewModelScope.launch { container.recentFiles.addFavorite(id, name) }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch { container.recentFiles.removeFavorite(id) }
    }

    val accent = container.recentFiles.accent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColor.DEFAULT)

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { container.recentFiles.setAccent(accent) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.recentFiles.setThemeMode(mode) }
    }

    fun refreshFolder() {
        val current = folderState.value ?: return
        viewModelScope.launch { rescanFolder(Uri.parse(current.treeUri)) }
    }

    private suspend fun rescanFolder(uri: Uri) {
        val label = runCatching { container.folderScanner.treeLabel(uri) }.getOrDefault(container.appContext.getString(R.string.selected_folder))
        folderState.update { FolderUiState(treeUri = uri.toString(), label = label, scanning = true) }
        val result = container.folderScanner.listMarkdown(uri)
        folderState.update {
            it?.copy(
                scanning = false,
                entries = result.entries,
                subfoldersScanned = result.subfoldersScanned,
                truncated = result.truncated,
            ) ?: FolderUiState(
                treeUri = uri.toString(),
                label = label,
                entries = result.entries,
                subfoldersScanned = result.subfoldersScanned,
                truncated = result.truncated,
            )
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReadMdApp
                HomeViewModel(app.container)
            }
        }
    }
}

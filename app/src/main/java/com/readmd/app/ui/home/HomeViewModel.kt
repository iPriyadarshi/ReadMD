package com.readmd.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.readmd.app.ReadMdApp
import com.readmd.app.data.FolderEntry
import com.readmd.app.data.FolderSort
import com.readmd.app.data.recent.RecentEntry
import com.readmd.app.di.AppContainer
import com.readmd.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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

class HomeViewModel(private val container: AppContainer) : ViewModel() {

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
                    FolderUiState(treeUri = it, label = "Selected folder", scanning = true)
                })?.copy(entries = sort.apply(scannedFolder?.entries.orEmpty())),
            folderSort = sort,
            themeMode = ThemeMode.fromOrdinal(themeOrdinal),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val samples = listOf(
        SampleDoc("res:welcome", "Welcome to ReadMD"),
        SampleDoc("res:cheatsheet", "Markdown Cheatsheet"),
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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.recentFiles.setThemeMode(mode) }
    }

    fun refreshFolder() {
        val current = folderState.value ?: return
        viewModelScope.launch { rescanFolder(Uri.parse(current.treeUri)) }
    }

    private suspend fun rescanFolder(uri: Uri) {
        val label = runCatching { container.folderScanner.treeLabel(uri) }.getOrDefault("Selected folder")
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
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReadMdApp
                HomeViewModel(app.container)
            }
        }
    }
}

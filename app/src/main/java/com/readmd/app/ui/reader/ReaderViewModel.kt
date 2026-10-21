package com.readmd.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.readmd.app.ReadMdApp
import com.readmd.app.di.AppContainer
import com.readmd.app.domain.Bookmark
import com.readmd.app.domain.HeadingNode
import com.readmd.app.domain.MarkdownOutlineParser
import com.readmd.app.domain.MathMarkup
import com.readmd.app.domain.MermaidBlocks
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.PageMargin
import com.readmd.app.domain.ReaderFont
import com.readmd.app.domain.ReadingTime
import com.readmd.app.domain.RelativeLink
import com.readmd.app.domain.TextScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val markdown: String? = null,
    /** [markdown] with "View diagram" links added before mermaid blocks; this is what gets rendered. */
    val renderedMarkdown: String? = null,
    /** Source of each mermaid block, indexed by its link. */
    val diagrams: List<String> = emptyList(),
    val headings: List<HeadingNode> = emptyList(),
    val rawMode: Boolean = false,
    val textScale: Int = TextScale.DEFAULT,
    val font: ReaderFont = ReaderFont.DEFAULT,
    val lineSpacing: LineSpacing = LineSpacing.DEFAULT,
    val pageMargin: PageMargin = PageMargin.DEFAULT,
    val wordCount: Int = 0,
    val keepScreenOn: Boolean = false,
    val isFavorite: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
)

/** Outcome of tapping a link in the rendered document. */
sealed interface LinkResult {
    /** A markdown file in the same folder; open it as another document. */
    data class Open(val docId: String) : LinkResult

    /** Not a relative file link (web URL, mail, anchor...); let the system handle it. */
    data class External(val link: String) : LinkResult

    /** A relative link that can't be followed; show [text] to the user. */
    data class Failed(val text: String) : LinkResult
}

class ReaderViewModel(
    private val docId: String,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            container.recentFiles.readerTextScale.collect { scale ->
                _state.update { it.copy(textScale = scale) }
            }
        }
        viewModelScope.launch {
            container.recentFiles.readerFont.collect { font ->
                _state.update { it.copy(font = font) }
            }
        }
        viewModelScope.launch {
            container.recentFiles.bookmarks(docId).collect { bookmarks ->
                _state.update { it.copy(bookmarks = bookmarks) }
            }
        }
        viewModelScope.launch {
            container.recentFiles.favorites.collect { favorites ->
                _state.update { it.copy(isFavorite = favorites.any { fav -> fav.id == docId }) }
            }
        }
        viewModelScope.launch {
            container.recentFiles.keepScreenOn.collect { enabled ->
                _state.update { it.copy(keepScreenOn = enabled) }
            }
        }
        viewModelScope.launch {
            container.recentFiles.lineSpacing.collect { spacing ->
                _state.update { it.copy(lineSpacing = spacing) }
            }
        }
        viewModelScope.launch {
            container.recentFiles.pageMargin.collect { margin ->
                _state.update { it.copy(pageMargin = margin) }
            }
        }
    }

    fun addBookmark(permille: Int, label: String) {
        viewModelScope.launch {
            container.recentFiles.addBookmark(Bookmark(docId, permille, label, System.currentTimeMillis()))
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        viewModelScope.launch { container.recentFiles.removeBookmark(docId, bookmark.permille) }
    }

    fun toggleFavorite() {
        val current = _state.value
        viewModelScope.launch {
            if (current.isFavorite) {
                container.recentFiles.removeFavorite(docId)
            } else {
                container.recentFiles.addFavorite(docId, current.title.ifEmpty { docId })
            }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { container.recentFiles.setKeepScreenOn(enabled) }
    }

    fun setLineSpacing(spacing: LineSpacing) {
        viewModelScope.launch { container.recentFiles.setLineSpacing(spacing) }
    }

    fun setPageMargin(margin: PageMargin) {
        viewModelScope.launch { container.recentFiles.setPageMargin(margin) }
    }

    fun setFont(font: ReaderFont) {
        viewModelScope.launch { container.recentFiles.setReaderFont(font) }
    }

    suspend fun savedPosition(): Int = container.recentFiles.readingPosition(docId) ?: 0

    fun savePosition(permille: Int) {
        viewModelScope.launch { container.recentFiles.saveReadingPosition(docId, permille) }
    }

    suspend fun resolveLink(link: String): LinkResult {
        if (!RelativeLink.isRelativeFileLink(link)) return LinkResult.External(link)

        val uri = android.net.Uri.parse(docId)
        if (docId.startsWith(com.readmd.app.data.DocumentRepository.BUNDLED_PREFIX) ||
            !android.provider.DocumentsContract.isTreeUri(uri)
        ) {
            return LinkResult.Failed("Relative links work for files opened from a folder")
        }
        val target = RelativeLink.resolve(android.provider.DocumentsContract.getDocumentId(uri), link)
            ?: return LinkResult.Failed("Can't follow this link: $link")
        if (!RelativeLink.isMarkdown(target)) {
            return LinkResult.Failed("Only markdown files can be opened from links")
        }
        val targetId = android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, target).toString()
        return if (container.documents.exists(targetId)) {
            LinkResult.Open(targetId)
        } else {
            LinkResult.Failed("File not found: ${link.substringBefore('#')}")
        }
    }

    fun retry() = load()

    fun adjustTextScale(steps: Int) {
        setTextScale(TextScale.step(_state.value.textScale, steps))
    }

    fun resetTextScale() = setTextScale(TextScale.DEFAULT)

    /** Live pinch feedback: updates the on-screen scale without persisting it yet. */
    fun pinchTextScale(zoom: Float) {
        _state.update { it.copy(textScale = TextScale.scaled(it.textScale, zoom)) }
    }

    /** Persists the scale reached by a finished pinch gesture. */
    fun commitTextScale() = setTextScale(_state.value.textScale)

    private fun setTextScale(percent: Int) {
        viewModelScope.launch { container.recentFiles.setReaderTextScale(percent) }
    }

    fun setRawMode(enabled: Boolean) {
        _state.update { it.copy(rawMode = enabled) }
    }

    private fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                if (!docId.startsWith(com.readmd.app.data.DocumentRepository.BUNDLED_PREFIX)) {
                    runCatching { container.documents.takePersistablePermission(android.net.Uri.parse(docId)) }
                }
                val doc = container.documents.read(docId)
                container.recentFiles.recordOpened(doc.id, doc.name)
                val prepared = MermaidBlocks.prepare(doc.text)
                _state.update {
                    it.copy(
                        loading = false,
                        title = doc.name,
                        markdown = doc.text,
                        renderedMarkdown = MathMarkup.prepare(prepared.markdown),
                        diagrams = prepared.diagrams,
                        headings = MarkdownOutlineParser.parse(doc.text),
                        wordCount = ReadingTime.wordCount(doc.text),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Could not open this file")
                }
            }
        }
    }

    companion object {
        fun factory(docId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReadMdApp
                ReaderViewModel(docId, app.container)
            }
        }
    }
}

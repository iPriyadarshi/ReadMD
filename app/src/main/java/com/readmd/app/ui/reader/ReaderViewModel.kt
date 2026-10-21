package com.readmd.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.readmd.app.ReadMdApp
import com.readmd.app.di.AppContainer
import com.readmd.app.domain.HeadingNode
import com.readmd.app.domain.MarkdownOutlineParser
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.PageMargin
import com.readmd.app.domain.ReaderFont
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
    val headings: List<HeadingNode> = emptyList(),
    val rawMode: Boolean = false,
    val textScale: Int = TextScale.DEFAULT,
    val font: ReaderFont = ReaderFont.DEFAULT,
    val lineSpacing: LineSpacing = LineSpacing.DEFAULT,
    val pageMargin: PageMargin = PageMargin.DEFAULT,
)

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

    fun setLineSpacing(spacing: LineSpacing) {
        viewModelScope.launch { container.recentFiles.setLineSpacing(spacing) }
    }

    fun setPageMargin(margin: PageMargin) {
        viewModelScope.launch { container.recentFiles.setPageMargin(margin) }
    }

    fun setFont(font: ReaderFont) {
        viewModelScope.launch { container.recentFiles.setReaderFont(font) }
    }

    fun retry() = load()

    fun adjustTextScale(steps: Int) {
        setTextScale(TextScale.step(_state.value.textScale, steps))
    }

    fun resetTextScale() = setTextScale(TextScale.DEFAULT)

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
                _state.update {
                    it.copy(
                        loading = false,
                        title = doc.name,
                        markdown = doc.text,
                        headings = MarkdownOutlineParser.parse(doc.text),
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

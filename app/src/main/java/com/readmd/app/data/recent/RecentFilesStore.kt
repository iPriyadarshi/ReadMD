package com.readmd.app.data.recent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.PageMargin
import com.readmd.app.domain.ReaderFont
import com.readmd.app.domain.ReadingPositions
import com.readmd.app.domain.TextScale
import com.readmd.app.ui.theme.AccentColor
import com.readmd.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefsStore by preferencesDataStore(name = "readmd_prefs")

data class RecentEntry(
    val id: String,
    val name: String,
    val lastOpened: Long,
)

/**
 * Persists recent files, the picked folder tree and the theme mode using
 * Preferences DataStore. Recent entries are stored as "timestamp\u001fname\u001fid"
 * strings in a string set (sets are unordered, so ordering is applied on read).
 */
class RecentFilesStore(private val context: Context) {

    private val dataStore get() = context.prefsStore

    val recents: Flow<List<RecentEntry>> = dataStore.data.map { prefs ->
        prefs[KEY_RECENTS].orEmpty()
            .mapNotNull { raw ->
                val parts = raw.split(SEP)
                if (parts.size != 3) null
                else RecentEntry(
                    id = parts[2],
                    name = parts[1],
                    lastOpened = parts[0].toLongOrNull() ?: 0L,
                )
            }
            .sortedByDescending { it.lastOpened }
            .take(MAX_RECENTS)
    }

    val folderTree: Flow<String?> = dataStore.data.map { it[KEY_FOLDER] }

    val folderSort: Flow<String> =
        dataStore.data.map { it[KEY_FOLDER_SORT] ?: DEFAULT_FOLDER_SORT }

    val themeMode: Flow<Int> = dataStore.data.map { it[KEY_THEME] ?: ThemeMode.SYSTEM.ordinal }

    val readerTextScale: Flow<Int> =
        dataStore.data.map { TextScale.clamp(it[KEY_TEXT_SCALE] ?: TextScale.DEFAULT) }

    val readerFont: Flow<ReaderFont> =
        dataStore.data.map { ReaderFont.fromId(it[KEY_READER_FONT]) }

    val lineSpacing: Flow<LineSpacing> =
        dataStore.data.map { LineSpacing.fromId(it[KEY_LINE_SPACING]) }

    val pageMargin: Flow<PageMargin> =
        dataStore.data.map { PageMargin.fromId(it[KEY_PAGE_MARGIN]) }

    val accent: Flow<AccentColor> = dataStore.data.map { AccentColor.fromId(it[KEY_ACCENT]) }

    suspend fun recordOpened(id: String, name: String) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_RECENTS].orEmpty()
                .filterNot { it.substringAfter(SEP, "").substringAfter(SEP) == id }
            val entry = "${System.currentTimeMillis()}$SEP$name$SEP$id"
            prefs[KEY_RECENTS] = (existing + entry)
                .distinct()
                .toSet()
        }
    }

    suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_RECENTS] = prefs[KEY_RECENTS].orEmpty()
                .filterNot { it.substringAfter(SEP, "").substringAfter(SEP) == id }
                .toSet()
        }
    }

    suspend fun setFolderTree(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(KEY_FOLDER) else prefs[KEY_FOLDER] = uri
        }
    }

    suspend fun setFolderSort(sortId: String) {
        dataStore.edit { it[KEY_FOLDER_SORT] = sortId }
    }

    suspend fun setAccent(accent: AccentColor) {
        dataStore.edit { it[KEY_ACCENT] = accent.id }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.ordinal }
    }

    suspend fun setReaderTextScale(percent: Int) {
        dataStore.edit { it[KEY_TEXT_SCALE] = TextScale.clamp(percent) }
    }

    suspend fun setReaderFont(font: ReaderFont) {
        dataStore.edit { it[KEY_READER_FONT] = font.id }
    }

    /** Last saved scroll position for [docId] in thousandths of the scroll range, or null. */
    suspend fun readingPosition(docId: String): Int? =
        ReadingPositions.get(dataStore.data.first()[KEY_POSITIONS].orEmpty(), docId)

    suspend fun saveReadingPosition(docId: String, permille: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_POSITIONS] = ReadingPositions.upsert(
                prefs[KEY_POSITIONS].orEmpty(), docId, permille, System.currentTimeMillis(),
            )
        }
    }

    suspend fun setLineSpacing(spacing: LineSpacing) {
        dataStore.edit { it[KEY_LINE_SPACING] = spacing.id }
    }

    suspend fun setPageMargin(margin: PageMargin) {
        dataStore.edit { it[KEY_PAGE_MARGIN] = margin.id }
    }

    companion object {
        private const val MAX_RECENTS = 20
        private const val DEFAULT_FOLDER_SORT = "NAME_ASC"
        private val SEP = "\u001f"
        private val KEY_RECENTS = stringSetPreferencesKey("recent_files")
        private val KEY_FOLDER = stringPreferencesKey("folder_tree_uri")
        private val KEY_FOLDER_SORT = stringPreferencesKey("folder_sort")
        private val KEY_THEME = intPreferencesKey("theme_mode")
        private val KEY_ACCENT = stringPreferencesKey("accent_color")
        private val KEY_TEXT_SCALE = intPreferencesKey("reader_text_scale")
        private val KEY_READER_FONT = stringPreferencesKey("reader_font")
        private val KEY_LINE_SPACING = stringPreferencesKey("reader_line_spacing")
        private val KEY_PAGE_MARGIN = stringPreferencesKey("reader_page_margin")
        private val KEY_POSITIONS = stringSetPreferencesKey("reading_positions")
    }
}

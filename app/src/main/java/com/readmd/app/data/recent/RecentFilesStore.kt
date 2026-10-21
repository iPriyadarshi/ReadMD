package com.readmd.app.data.recent

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.readmd.app.domain.Bookmark
import com.readmd.app.domain.Bookmarks
import com.readmd.app.domain.CodeWrap
import com.readmd.app.domain.CollectionItem
import com.readmd.app.domain.Collections
import com.readmd.app.domain.DocCollection
import com.readmd.app.domain.FavoriteEntry
import com.readmd.app.domain.Favorites
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.PageMargin
import com.readmd.app.domain.ReaderFont
import com.readmd.app.domain.ReadingPositions
import com.readmd.app.domain.TextScale
import com.readmd.app.ui.theme.AccentColor
import com.readmd.app.ui.theme.ThemeMode
import java.util.UUID
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

    val collections: Flow<List<DocCollection>> =
        dataStore.data.map { Collections.parseCollections(it[KEY_COLLECTIONS].orEmpty()) }

    val collectionItems: Flow<List<CollectionItem>> =
        dataStore.data.map { Collections.parseItems(it[KEY_COLLECTION_ITEMS].orEmpty()) }

    fun bookmarks(docId: String): Flow<List<Bookmark>> =
        dataStore.data.map { Bookmarks.forDoc(it[KEY_BOOKMARKS].orEmpty(), docId) }

    val favorites: Flow<List<FavoriteEntry>> =
        dataStore.data.map { Favorites.parse(it[KEY_FAVORITES].orEmpty()) }

    val codeWrap: Flow<CodeWrap> = dataStore.data.map { CodeWrap.fromId(it[KEY_CODE_WRAP]) }

    val autosave: Flow<Boolean> = dataStore.data.map { it[KEY_AUTOSAVE] ?: true }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: false }

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

    /** Creates a collection and returns its id. */
    suspend fun createCollection(name: String): String {
        val id = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTIONS] = Collections.create(prefs[KEY_COLLECTIONS].orEmpty(), id, name)
        }
        return id
    }

    suspend fun renameCollection(id: String, name: String) {
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTIONS] = Collections.rename(prefs[KEY_COLLECTIONS].orEmpty(), id, name)
        }
    }

    /** Deletes the collection and forgets which documents were in it (the documents are untouched). */
    suspend fun deleteCollection(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTIONS] = Collections.delete(prefs[KEY_COLLECTIONS].orEmpty(), id)
            prefs[KEY_COLLECTION_ITEMS] = Collections.deleteItems(prefs[KEY_COLLECTION_ITEMS].orEmpty(), id)
        }
    }

    suspend fun addToCollection(collectionId: String, docId: String, name: String) {
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTION_ITEMS] =
                Collections.assign(prefs[KEY_COLLECTION_ITEMS].orEmpty(), collectionId, docId, name)
        }
    }

    suspend fun removeFromCollection(collectionId: String, docId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTION_ITEMS] =
                Collections.unassign(prefs[KEY_COLLECTION_ITEMS].orEmpty(), collectionId, docId)
        }
    }

    suspend fun addBookmark(bookmark: Bookmark) {
        dataStore.edit { prefs ->
            prefs[KEY_BOOKMARKS] = Bookmarks.add(prefs[KEY_BOOKMARKS].orEmpty(), bookmark)
        }
    }

    suspend fun removeBookmark(docId: String, permille: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BOOKMARKS] = Bookmarks.remove(prefs[KEY_BOOKMARKS].orEmpty(), docId, permille)
        }
    }

    suspend fun addFavorite(id: String, name: String) {
        dataStore.edit { prefs ->
            prefs[KEY_FAVORITES] =
                Favorites.add(prefs[KEY_FAVORITES].orEmpty(), id, name, System.currentTimeMillis())
        }
    }

    suspend fun removeFavorite(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_FAVORITES] = Favorites.remove(prefs[KEY_FAVORITES].orEmpty(), id)
        }
    }

    suspend fun setCodeWrap(wrap: CodeWrap) {
        dataStore.edit { it[KEY_CODE_WRAP] = wrap.id }
    }

    suspend fun setAutosave(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTOSAVE] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
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
        private val KEY_CODE_WRAP = stringPreferencesKey("code_wrap")
        private val KEY_AUTOSAVE = booleanPreferencesKey("autosave")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_files")
        private val KEY_BOOKMARKS = stringSetPreferencesKey("bookmarks")
        private val KEY_COLLECTIONS = stringSetPreferencesKey("collections")
        private val KEY_COLLECTION_ITEMS = stringSetPreferencesKey("collection_items")
        private val KEY_TEXT_SCALE = intPreferencesKey("reader_text_scale")
        private val KEY_READER_FONT = stringPreferencesKey("reader_font")
        private val KEY_LINE_SPACING = stringPreferencesKey("reader_line_spacing")
        private val KEY_PAGE_MARGIN = stringPreferencesKey("reader_page_margin")
        private val KEY_POSITIONS = stringSetPreferencesKey("reading_positions")
    }
}

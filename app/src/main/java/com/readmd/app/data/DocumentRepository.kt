package com.readmd.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class LoadedDocument(
    val id: String,
    val name: String,
    val text: String,
)

/**
 * Reads markdown documents from two kinds of sources, addressed by a single
 * string id:
 *  - "res:<name>"   -> a bundled sample in res/raw
 *  - anything else  -> a content:// (or file://) URI string from SAF
 *
 * Designed so a future source (e.g. http) only needs a new prefix branch.
 */
class DocumentRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    suspend fun read(id: String): LoadedDocument = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) {
            readBundled(id.removePrefix(BUNDLED_PREFIX))
        } else {
            readContentUri(Uri.parse(id))
        }
    }

    /** Best-effort display name without reading the whole document. */
    suspend fun displayNameOf(id: String): String = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) {
            id.removePrefix(BUNDLED_PREFIX)
                .removeSuffix(".md")
                .replaceFirstChar { it.uppercase() }
        } else {
            queryDisplayName(Uri.parse(id)) ?: "Untitled.md"
        }
    }

    /** Keeps persistent read access to a SAF uri so recents keep working. */
    fun takePersistablePermission(uri: Uri) {
        try {
            resolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // Not all providers offer persistable grants; in-memory grant is
            // still valid for this session.
        }
    }

    private fun readBundled(name: String): LoadedDocument {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId == 0) throw IOException("Bundled document \"$name\" not found")
        val text = context.resources.openRawResource(resId)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return LoadedDocument(id = BUNDLED_PREFIX + name, name = name, text = text)
    }

    private fun readContentUri(uri: Uri): LoadedDocument {
        val text = runCatching {
            if (uri.scheme == "file") {
                uri.path?.let { java.io.File(it).readText(Charsets.UTF_8) }
            } else {
                resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }
        }.getOrElse { throw IOException("Cannot read this file: ${it.message}") }
            ?: throw IOException("Cannot open this file")

        val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: "Untitled.md"
        return LoadedDocument(id = uri.toString(), name = name, text = text)
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    }.getOrNull()

    companion object {
        const val BUNDLED_PREFIX = "res:"
    }
}

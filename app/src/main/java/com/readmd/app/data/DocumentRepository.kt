package com.readmd.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.readmd.app.R
import com.readmd.app.domain.DocumentStamp
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

    /** True when [id] can be opened (used to detect broken links before navigating). */
    suspend fun exists(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) {
            context.resources.getIdentifier(id.removePrefix(BUNDLED_PREFIX), "raw", context.packageName) != 0
        } else {
            queryDisplayName(Uri.parse(id)) != null
        }
    }

    /**
     * Keeps persistent access to a SAF uri so recents keep working, including write access
     * when the provider granted it (needed for editing). Falls back to read-only.
     */
    fun takePersistablePermission(uri: Uri) {
        val read = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        val write = android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            resolver.takePersistableUriPermission(uri, read or write)
        } catch (_: Exception) {
            try {
                resolver.takePersistableUriPermission(uri, read)
            } catch (_: Exception) {
                // Not all providers offer persistable grants; in-memory grant is
                // still valid for this session.
            }
        }
    }

    /**
     * The document's current modified time and size, or null when they can't be read (bundled
     * samples, providers that don't report them, files that are gone).
     */
    suspend fun stamp(id: String): DocumentStamp? = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) return@withContext null
        runCatching {
            resolver.query(
                Uri.parse(id),
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    android.provider.DocumentsContract.Document.COLUMN_SIZE,
                ),
                null, null, null,
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                DocumentStamp(
                    lastModified = if (c.isNull(0)) null else c.getLong(0),
                    size = if (c.isNull(1)) null else c.getLong(1),
                ).takeUnless { it.isEmpty }
            }
        }.getOrNull()
    }

    /** True for documents that live in a file the user picked (bundled samples are read-only). */
    fun isEditable(id: String): Boolean = !id.startsWith(BUNDLED_PREFIX)

    /**
     * Replaces the document's contents with [text].
     * @throws IOException with a readable message when the file can't be written
     */
    suspend fun write(id: String, text: String) = withContext(Dispatchers.IO) {
        if (!isEditable(id)) throw IOException(context.getString(R.string.error_bundled_readonly))
        try {
            val stream = resolver.openOutputStream(Uri.parse(id), "wt")
                ?: throw IOException(context.getString(R.string.error_write_unavailable))
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        } catch (e: SecurityException) {
            throw IOException(context.getString(R.string.error_write_permission))
        } catch (e: java.io.FileNotFoundException) {
            throw IOException(context.getString(R.string.error_write_readonly))
        }
    }

    private fun readBundled(name: String): LoadedDocument {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId == 0) throw IOException(context.getString(R.string.error_bundled_missing, name))
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
        }.getOrElse { throw IOException(context.getString(R.string.error_cannot_read, it.message.orEmpty())) }
            ?: throw IOException(context.getString(R.string.error_cannot_open))

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

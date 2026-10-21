package com.readmd.app.data

import android.net.Uri
import com.readmd.app.domain.DocumentStamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
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
 * Designed so a future source (e.g. http) only needs a new prefix branch. Raw access goes
 * through [DocumentSource], and failure wording through [DocumentMessages].
 */
class DocumentRepository(
    private val source: DocumentSource,
    private val messages: DocumentMessages,
) {

    suspend fun read(id: String): LoadedDocument = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) {
            readBundled(id.removePrefix(BUNDLED_PREFIX))
        } else {
            readFromSource(id)
        }
    }

    /** Best-effort display name without reading the whole document. */
    suspend fun displayNameOf(id: String): String = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) {
            id.removePrefix(BUNDLED_PREFIX)
                .removeSuffix(".md")
                .replaceFirstChar { it.uppercase() }
        } else {
            source.displayName(id) ?: UNTITLED
        }
    }

    /** True when [id] can be opened (used to detect broken links before navigating). */
    suspend fun exists(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) {
            source.bundledText(id.removePrefix(BUNDLED_PREFIX)) != null
        } else {
            source.displayName(id) != null
        }
    }

    /**
     * Keeps persistent access to a SAF uri so recents keep working, including write access
     * when the provider granted it (needed for editing). Falls back to read-only.
     */
    fun takePersistablePermission(uri: Uri) = source.takePersistablePermission(uri.toString())

    /**
     * The document's current modified time and size, or null when they can't be read (bundled
     * samples, providers that don't report them, files that are gone).
     */
    suspend fun stamp(id: String): DocumentStamp? = withContext(Dispatchers.IO) {
        if (id.startsWith(BUNDLED_PREFIX)) null else source.stamp(id)
    }

    /** True for documents that live in a file the user picked (bundled samples are read-only). */
    fun isEditable(id: String): Boolean = !id.startsWith(BUNDLED_PREFIX)

    /**
     * Replaces the document's contents with [text].
     * @throws IOException with a readable message when the file can't be written
     */
    suspend fun write(id: String, text: String) = withContext(Dispatchers.IO) {
        if (!isEditable(id)) throw IOException(messages.bundledReadOnly())
        try {
            val stream = source.openForWriting(id) ?: throw IOException(messages.writeUnavailable())
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        } catch (e: SecurityException) {
            throw IOException(messages.writePermission())
        } catch (e: FileNotFoundException) {
            throw IOException(messages.writeReadOnly())
        }
    }

    private fun readBundled(name: String): LoadedDocument {
        val text = source.bundledText(name) ?: throw IOException(messages.bundledMissing(name))
        return LoadedDocument(id = BUNDLED_PREFIX + name, name = name, text = text)
    }

    private fun readFromSource(id: String): LoadedDocument {
        val text = try {
            source.openForReading(id)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } catch (e: Exception) {
            throw IOException(messages.cannotRead(e.message.orEmpty()))
        } ?: throw IOException(messages.cannotOpen())

        val name = source.displayName(id) ?: source.lastPathSegment(id) ?: UNTITLED
        return LoadedDocument(id = id, name = name, text = text)
    }

    companion object {
        const val BUNDLED_PREFIX = "res:"
        private const val UNTITLED = "Untitled.md"
    }
}

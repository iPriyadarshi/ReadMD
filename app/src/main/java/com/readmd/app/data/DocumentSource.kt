package com.readmd.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.readmd.app.R
import com.readmd.app.domain.DocumentStamp
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import android.provider.DocumentsContract

/**
 * Where document bytes and details come from. [DocumentRepository] holds the rules (what a
 * bundled sample is, how names fall back, which errors mean what) and asks this for the raw
 * access, so those rules can be tested with a fake and no Android framework.
 */
interface DocumentSource {
    /** The stream for reading [id]; null when it can't be opened. May throw if access is refused. */
    fun openForReading(id: String): InputStream?

    /** A stream that replaces the contents of [id]; null when it can't be opened for writing. */
    fun openForWriting(id: String): OutputStream?

    /** The name the provider reports for [id], or null when it reports none. */
    fun displayName(id: String): String?

    /** The last part of the address, used as a name when the provider reports none. */
    fun lastPathSegment(id: String): String?

    fun stamp(id: String): DocumentStamp?

    /** The text of the bundled sample called [name], or null when there is none. */
    fun bundledText(name: String): String?

    fun takePersistablePermission(id: String)
}

/** User-visible wording for the ways reading and writing documents fail. */
interface DocumentMessages {
    fun bundledReadOnly(): String
    fun writeUnavailable(): String
    fun writePermission(): String
    fun writeReadOnly(): String
    fun bundledMissing(name: String): String
    fun cannotRead(reason: String): String
    fun cannotOpen(): String
}

/** [DocumentSource] backed by the ContentResolver and the app's bundled resources. */
class AndroidDocumentSource(private val context: Context) : DocumentSource {

    private val resolver get() = context.contentResolver

    override fun openForReading(id: String): InputStream? {
        val uri = Uri.parse(id)
        return if (uri.scheme == "file") {
            uri.path?.let { File(it).inputStream() }
        } else {
            resolver.openInputStream(uri)
        }
    }

    override fun openForWriting(id: String): OutputStream? = resolver.openOutputStream(Uri.parse(id), "wt")

    override fun displayName(id: String): String? = runCatching {
        resolver.query(Uri.parse(id), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    }.getOrNull()

    override fun lastPathSegment(id: String): String? = Uri.parse(id).lastPathSegment

    override fun stamp(id: String): DocumentStamp? = runCatching {
        resolver.query(
            Uri.parse(id),
            arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_SIZE),
            null, null, null,
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            DocumentStamp(
                lastModified = if (c.isNull(0)) null else c.getLong(0),
                size = if (c.isNull(1)) null else c.getLong(1),
            ).takeUnless { it.isEmpty }
        }
    }.getOrNull()

    override fun bundledText(name: String): String? {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId == 0) return null
        return context.resources.openRawResource(resId).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    override fun takePersistablePermission(id: String) {
        val uri = Uri.parse(id)
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
}

class AndroidDocumentMessages(private val context: Context) : DocumentMessages {
    override fun bundledReadOnly() = context.getString(R.string.error_bundled_readonly)
    override fun writeUnavailable() = context.getString(R.string.error_write_unavailable)
    override fun writePermission() = context.getString(R.string.error_write_permission)
    override fun writeReadOnly() = context.getString(R.string.error_write_readonly)
    override fun bundledMissing(name: String) = context.getString(R.string.error_bundled_missing, name)
    override fun cannotRead(reason: String) = context.getString(R.string.error_cannot_read, reason)
    override fun cannotOpen() = context.getString(R.string.error_cannot_open)
}

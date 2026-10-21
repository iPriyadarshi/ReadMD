package com.readmd.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.readmd.app.domain.SharedNames
import java.io.File
import java.io.IOException

/**
 * Turns content shared into the app (text or a file) into a document the reader can open.
 * Shared content comes with temporary access only, so it is copied into the app's own storage;
 * that copy stays readable later and can be edited. Run on a background thread.
 */
class SharedContentImporter(private val context: Context) {

    private val directory: File get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    /** The document id for what [intent] shares, or null when there is nothing usable in it. */
    fun importFrom(intent: Intent): String? = runCatching {
        val stream = streamOf(intent)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        val file = when {
            stream != null -> copyStream(stream)
            !text.isNullOrBlank() -> writeText(intent.getStringExtra(Intent.EXTRA_SUBJECT), text)
            else -> null
        }
        file?.also { prune() }?.let { Uri.fromFile(it).toString() }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun streamOf(intent: Intent): Uri? =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }

    private fun copyStream(uri: Uri): File {
        val name = SharedNames.forFile(displayName(uri) ?: uri.lastPathSegment)
        val target = File(directory, SharedNames.unique(name, existingNames()))
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read the shared file")
        input.use { source ->
            target.outputStream().use { sink ->
                var copied = 0L
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    copied += read
                    if (copied > MAX_BYTES) {
                        target.delete()
                        throw IOException("The shared file is too large")
                    }
                    sink.write(buffer, 0, read)
                }
            }
        }
        return target
    }

    private fun writeText(subject: String?, text: String): File {
        val target = File(directory, SharedNames.unique(SharedNames.forText(subject), existingNames()))
        target.writeText(text, Charsets.UTF_8)
        return target
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    }.getOrNull()

    private fun existingNames(): Set<String> = directory.list()?.toSet().orEmpty()

    /** Keeps the folder from growing without bound: only the newest imports are kept. */
    private fun prune() {
        val files = directory.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_FILES).forEach { it.delete() }
    }

    private companion object {
        const val DIRECTORY = "shared"
        const val MAX_FILES = 30
        const val MAX_BYTES = 10L * 1024 * 1024
    }
}

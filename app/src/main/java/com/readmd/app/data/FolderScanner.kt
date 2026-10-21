package com.readmd.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.readmd.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FolderEntry(
    /** Document uri string, usable directly as a document id. */
    val id: String,
    val name: String,
    /** Last modified epoch millis, 0 when the provider does not report it. */
    val lastModified: Long,
)

/** Result of a recursive scan of a picked folder tree. */
data class FolderScanResult(
    val entries: List<FolderEntry>,
    /** How many subfolders were descended into (0 = top level only). */
    val subfoldersScanned: Int,
    /** True when the [limit] cap was hit and results are truncated. */
    val truncated: Boolean,
)

/**
 * Recursively lists markdown files under a SAF document tree using
 * DocumentsContract directly (no DocumentFile dependency).
 */
class FolderScanner(private val context: Context) {

    /**
     * @return markdown files found recursively under the tree, capped at
     * [limit] so huge trees cannot freeze the UI.
     */
    suspend fun listMarkdown(treeUri: Uri, limit: Int = 300): FolderScanResult =
        withContext(Dispatchers.IO) {
            val root = DocumentsContract.getTreeDocumentId(treeUri)
            val out = ArrayList<FolderEntry>()
            var subfolders = 0
            try {
                scan(treeUri, root, out, limit) { subfolders++ }
            } catch (_: SecurityException) {
                // Permission lost; caller surfaces an empty list + error state.
            }
            FolderScanResult(
                entries = out,
                subfoldersScanned = subfolders,
                truncated = out.size >= limit,
            )
        }

    /** Display name of the tree root, used as the folder section header. */
    suspend fun treeLabel(treeUri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            context.contentResolver.query(
                DocumentsContract.buildDocumentUriUsingTree(treeUri, docId),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
            }
        }.getOrNull() ?: context.getString(R.string.selected_folder)
    }

    private fun scan(
        tree: Uri,
        documentId: String,
        out: MutableList<FolderEntry>,
        limit: Int,
        onSubfolder: () -> Unit,
    ) {
        if (out.size >= limit) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(2)
                val lastModified = if (c.isNull(3)) 0L else c.getLong(3)
                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        onSubfolder()
                        scan(tree, id, out, limit, onSubfolder)
                    }
                    isMarkdown(name, mime) -> out += FolderEntry(
                        id = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                        name = name,
                        lastModified = lastModified,
                    )
                }
            }
        }
    }

    private fun isMarkdown(name: String, mime: String?): Boolean =
        name.endsWith(".md", ignoreCase = true) ||
            name.endsWith(".markdown", ignoreCase = true) ||
            mime == "text/markdown" ||
            mime == "text/x-markdown"
}

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

/** One item in a folder, as a document provider describes it. */
data class TreeChild(
    val id: String,
    /** Null when the provider gives the item no name; such items are skipped. */
    val name: String?,
    val isDirectory: Boolean,
    val mimeType: String?,
    val lastModified: Long,
)

/** A folder hierarchy that can be listed one level at a time. */
interface DocumentTree {
    /** The items directly inside the folder [documentId], or null when it can't be listed. */
    fun children(documentId: String): List<TreeChild>?

    /** The id under which the item [documentId] is opened (a document uri string). */
    fun openId(documentId: String): String
}

/** Which names and types count as markdown documents. */
object MarkdownFiles {
    fun matches(name: String, mimeType: String?): Boolean =
        name.endsWith(".md", ignoreCase = true) ||
            name.endsWith(".markdown", ignoreCase = true) ||
            mimeType == "text/markdown" ||
            mimeType == "text/x-markdown"
}

/**
 * Walks a [DocumentTree] depth-first and collects the markdown files in it. Folders that
 * can't be listed are skipped, a lost permission ends the walk with what was found so far,
 * and the walk stops once [limit] files are collected so a huge tree can't freeze the UI.
 */
class TreeScanner(private val tree: DocumentTree) {

    fun scan(rootId: String, limit: Int): FolderScanResult {
        val out = ArrayList<FolderEntry>()
        var subfolders = 0
        try {
            walk(rootId, out, limit) { subfolders++ }
        } catch (_: SecurityException) {
            // Permission lost; the caller shows whatever was found.
        }
        return FolderScanResult(
            entries = out,
            subfoldersScanned = subfolders,
            truncated = out.size >= limit,
        )
    }

    private fun walk(documentId: String, out: MutableList<FolderEntry>, limit: Int, onSubfolder: () -> Unit) {
        if (out.size >= limit) return
        val children = tree.children(documentId) ?: return
        for (child in children) {
            if (out.size >= limit) return
            val name = child.name ?: continue
            when {
                child.isDirectory -> {
                    onSubfolder()
                    walk(child.id, out, limit, onSubfolder)
                }
                MarkdownFiles.matches(name, child.mimeType) -> out += FolderEntry(
                    id = tree.openId(child.id),
                    name = name,
                    lastModified = child.lastModified,
                )
            }
        }
    }
}

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
            TreeScanner(ContentResolverTree(context, treeUri)).scan(root, limit)
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
}

/** [DocumentTree] over a Storage Access Framework tree uri. */
private class ContentResolverTree(
    private val context: Context,
    private val treeUri: Uri,
) : DocumentTree {

    override fun children(documentId: String): List<TreeChild>? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        return context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null, null, null,
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val mime = c.getString(2)
                    add(
                        TreeChild(
                            id = c.getString(0),
                            name = c.getString(1),
                            isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                            mimeType = mime,
                            lastModified = if (c.isNull(3)) 0L else c.getLong(3),
                        ),
                    )
                }
            }
        }
    }

    override fun openId(documentId: String): String =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString()
}

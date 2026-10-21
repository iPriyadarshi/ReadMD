package com.readmd.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeScannerTest {

    /** A folder hierarchy held in memory. A folder missing from [folders] can't be listed. */
    private class FakeTree(
        private val folders: Map<String, List<TreeChild>>,
        private val refused: Set<String> = emptySet(),
    ) : DocumentTree {
        val listed = mutableListOf<String>()

        override fun children(documentId: String): List<TreeChild>? {
            listed += documentId
            if (documentId in refused) throw SecurityException("permission lost for $documentId")
            return folders[documentId]
        }

        override fun openId(documentId: String): String = "doc://$documentId"
    }

    private fun file(id: String, name: String, mime: String? = null, modified: Long = 0L) =
        TreeChild(id, name, isDirectory = false, mimeType = mime, lastModified = modified)

    private fun folder(id: String, name: String = id) =
        TreeChild(id, name, isDirectory = true, mimeType = "vnd.android.document/directory", lastModified = 0L)

    private fun scan(tree: FakeTree, limit: Int = 300) = TreeScanner(tree).scan("root", limit)

    @Test
    fun `an empty folder gives an empty result`() {
        val result = scan(FakeTree(mapOf("root" to emptyList())))
        assertTrue(result.entries.isEmpty())
        assertEquals(0, result.subfoldersScanned)
        assertFalse(result.truncated)
    }

    @Test
    fun `markdown files at the top level are listed with their id name and time`() {
        val result = scan(FakeTree(mapOf("root" to listOf(file("a", "Notes.md", modified = 42L)))))
        assertEquals(listOf(FolderEntry("doc://a", "Notes.md", 42L)), result.entries)
    }

    @Test
    fun `only markdown files are kept`() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(
                    file("1", "readme.md"),
                    file("2", "photo.png", mime = "image/png"),
                    file("3", "notes.txt", mime = "text/plain"),
                    file("4", "guide.markdown"),
                    file("5", "UPPER.MD"),
                ),
            ),
        )
        assertEquals(listOf("readme.md", "guide.markdown", "UPPER.MD"), scan(tree).entries.map { it.name })
    }

    @Test
    fun `files are recognised by markdown mime types even without an extension`() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(
                    file("1", "plain-name", mime = "text/markdown"),
                    file("2", "other", mime = "text/x-markdown"),
                    file("3", "skipped", mime = "application/pdf"),
                ),
            ),
        )
        assertEquals(listOf("plain-name", "other"), scan(tree).entries.map { it.name })
    }

    @Test
    fun `subfolders are searched recursively and counted`() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(file("a", "top.md"), folder("docs"), folder("empty")),
                "docs" to listOf(file("b", "guide.md"), folder("docs/deep")),
                "docs/deep" to listOf(file("c", "deep.md")),
                "empty" to emptyList(),
            ),
        )
        val result = scan(tree)
        assertEquals(listOf("top.md", "guide.md", "deep.md"), result.entries.map { it.name })
        assertEquals(3, result.subfoldersScanned)
        assertEquals(listOf("doc://a", "doc://b", "doc://c"), result.entries.map { it.id })
    }

    @Test
    fun `items without a name are skipped`() {
        val tree = FakeTree(mapOf("root" to listOf(TreeChild("x", null, false, "text/markdown", 0L), file("a", "ok.md"))))
        assertEquals(listOf("ok.md"), scan(tree).entries.map { it.name })
    }

    @Test
    fun `reaching the limit stops the scan and marks the result truncated`() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(file("1", "a.md"), file("2", "b.md"), folder("more"), file("3", "c.md")),
                "more" to listOf(file("4", "d.md")),
            ),
        )
        val result = scan(tree, limit = 2)
        assertEquals(listOf("a.md", "b.md"), result.entries.map { it.name })
        assertTrue(result.truncated)
        assertFalse("must not descend once the limit is reached", "more" in tree.listed)
    }

    @Test
    fun `finding exactly the limit counts as truncated but a smaller result does not`() {
        val tree = FakeTree(mapOf("root" to listOf(file("1", "a.md"), file("2", "b.md"))))
        assertTrue(scan(tree, limit = 2).truncated)
        assertFalse(scan(tree, limit = 3).truncated)
    }

    @Test
    fun `a folder that can't be listed is skipped and the rest is still scanned`() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(folder("broken"), file("a", "kept.md")),
                // "broken" is deliberately absent, so listing it gives null.
            ),
        )
        val result = scan(tree)
        assertEquals(listOf("kept.md"), result.entries.map { it.name })
        assertEquals(1, result.subfoldersScanned)
    }

    @Test
    fun `losing permission part way keeps what was found so far`() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(file("a", "first.md"), folder("locked"), file("b", "never reached.md")),
                "locked" to listOf(file("c", "hidden.md")),
            ),
            refused = setOf("locked"),
        )
        val result = scan(tree)
        assertEquals(listOf("first.md"), result.entries.map { it.name })
        assertFalse(result.truncated)
    }

    @Test
    fun `losing permission at the root gives an empty result instead of an error`() {
        val tree = FakeTree(emptyMap(), refused = setOf("root"))
        val result = scan(tree)
        assertTrue(result.entries.isEmpty())
    }
}

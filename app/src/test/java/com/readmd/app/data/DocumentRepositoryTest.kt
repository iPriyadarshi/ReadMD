package com.readmd.app.data

import com.readmd.app.domain.DocumentStamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DocumentRepositoryTest {

    /** Documents, names and failures held in memory. */
    private class FakeSource : DocumentSource {
        val bundled = mutableMapOf<String, String>()
        val contents = mutableMapOf<String, ByteArray>()
        val names = mutableMapOf<String, String>()
        val stamps = mutableMapOf<String, DocumentStamp>()
        val written = mutableMapOf<String, ByteArrayOutputStream>()
        var readFailure: Exception? = null
        var writeFailure: Exception? = null
        var writable = true
        var persistedFor: String? = null

        override fun openForReading(id: String): InputStream? {
            readFailure?.let { throw it }
            return contents[id]?.let { ByteArrayInputStream(it) }
        }

        override fun openForWriting(id: String): OutputStream? {
            writeFailure?.let { throw it }
            if (!writable) return null
            return ByteArrayOutputStream().also { written[id] = it }
        }

        override fun displayName(id: String): String? = names[id]
        override fun lastPathSegment(id: String): String? = id.substringAfterLast('/').takeIf { it.isNotEmpty() }
        override fun stamp(id: String): DocumentStamp? = stamps[id]
        override fun bundledText(name: String): String? = bundled[name]
        override fun takePersistablePermission(id: String) {
            persistedFor = id
        }
    }

    private class FakeMessages : DocumentMessages {
        override fun bundledReadOnly() = "bundled-readonly"
        override fun writeUnavailable() = "write-unavailable"
        override fun writePermission() = "write-permission"
        override fun writeReadOnly() = "write-readonly"
        override fun bundledMissing(name: String) = "bundled-missing:$name"
        override fun cannotRead(reason: String) = "cannot-read:$reason"
        override fun cannotOpen() = "cannot-open"
    }

    private val source = FakeSource()
    private val repository = DocumentRepository(source, FakeMessages())

    private fun failure(block: suspend () -> Unit): IOException {
        try {
            runBlocking { block() }
        } catch (e: IOException) {
            return e
        }
        fail("expected an IOException")
        throw AssertionError()
    }

    // ---- reading

    @Test
    fun `a bundled sample is read by name`() {
        source.bundled["welcome"] = "# Hi"
        val doc = runBlocking { repository.read("res:welcome") }
        assertEquals(LoadedDocument("res:welcome", "welcome", "# Hi"), doc)
    }

    @Test
    fun `a missing bundled sample fails with its name`() {
        assertEquals("bundled-missing:nope", failure { repository.read("res:nope") }.message)
    }

    @Test
    fun `a picked document is decoded as utf-8 and named by the provider`() {
        source.contents["doc://1"] = "Café ☕".toByteArray(Charsets.UTF_8)
        source.names["doc://1"] = "Cafe.md"
        val doc = runBlocking { repository.read("doc://1") }
        assertEquals(LoadedDocument("doc://1", "Cafe.md", "Café ☕"), doc)
    }

    @Test
    fun `a document without a reported name falls back to the end of its address`() {
        source.contents["doc://tree/x/notes.md"] = "a".toByteArray()
        assertEquals("notes.md", runBlocking { repository.read("doc://tree/x/notes.md") }.name)
    }

    @Test
    fun `a document with no usable name is called Untitled`() {
        source.contents["doc://"] = "a".toByteArray()
        assertEquals("Untitled.md", runBlocking { repository.read("doc://") }.name)
    }

    @Test
    fun `a document that can't be opened fails with the generic message`() {
        assertEquals("cannot-open", failure { repository.read("doc://gone") }.message)
    }

    @Test
    fun `a failure while reading is reported with its reason`() {
        source.readFailure = SecurityException("denied")
        assertEquals("cannot-read:denied", failure { repository.read("doc://1") }.message)
    }

    // ---- names and existence

    @Test
    fun `bundled names are tidied for display`() {
        assertEquals("Welcome", runBlocking { repository.displayNameOf("res:welcome.md") })
        assertEquals("Cheatsheet", runBlocking { repository.displayNameOf("res:cheatsheet") })
    }

    @Test
    fun `display names come from the provider or default to Untitled`() {
        source.names["doc://1"] = "Plan.md"
        assertEquals("Plan.md", runBlocking { repository.displayNameOf("doc://1") })
        assertEquals("Untitled.md", runBlocking { repository.displayNameOf("doc://unknown") })
    }

    @Test
    fun `a document exists when its sample is bundled or the provider knows its name`() {
        source.bundled["welcome"] = "x"
        source.names["doc://1"] = "a.md"
        assertTrue(runBlocking { repository.exists("res:welcome") })
        assertFalse(runBlocking { repository.exists("res:missing") })
        assertTrue(runBlocking { repository.exists("doc://1") })
        assertFalse(runBlocking { repository.exists("doc://2") })
    }

    // ---- change detection

    @Test
    fun `stamps come from the provider and bundled samples have none`() {
        source.stamps["doc://1"] = DocumentStamp(5L, 10L)
        assertEquals(DocumentStamp(5L, 10L), runBlocking { repository.stamp("doc://1") })
        assertNull(runBlocking { repository.stamp("doc://2") })
        assertNull(runBlocking { repository.stamp("res:welcome") })
    }

    // ---- writing

    @Test
    fun `only picked documents are editable`() {
        assertTrue(repository.isEditable("doc://1"))
        assertFalse(repository.isEditable("res:welcome"))
    }

    @Test
    fun `writing replaces the document with the utf-8 bytes`() {
        runBlocking { repository.write("doc://1", "Neu ✓") }
        assertEquals("Neu ✓", source.written.getValue("doc://1").toString(Charsets.UTF_8))
    }

    @Test
    fun `bundled samples can't be written`() {
        assertEquals("bundled-readonly", failure { repository.write("res:welcome", "x") }.message)
        assertTrue(source.written.isEmpty())
    }

    @Test
    fun `a document that can't be opened for writing fails clearly`() {
        source.writable = false
        assertEquals("write-unavailable", failure { repository.write("doc://1", "x") }.message)
    }

    @Test
    fun `a refused write is reported as a permission problem`() {
        source.writeFailure = SecurityException("no")
        assertEquals("write-permission", failure { repository.write("doc://1", "x") }.message)
    }

    @Test
    fun `a read-only file is reported as such`() {
        source.writeFailure = FileNotFoundException("EACCES")
        assertEquals("write-readonly", failure { repository.write("doc://1", "x") }.message)
    }
}

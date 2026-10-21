package com.readmd.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(keep: Int = CrashStore.MAX_REPORTS) = CrashStore(File(folder.root, "crashes"), keep)

    @Test
    fun `there is nothing to report before a crash`() {
        assertNull(store().latest())
    }

    @Test
    fun `a saved report is read back`() {
        val store = store()
        store.save("first crash", nowMillis = 100L)
        assertEquals("first crash", store.latest())
    }

    @Test
    fun `the latest report is the newest one`() {
        val store = store()
        store.save("old", 100L)
        store.save("new", 200L)
        store.save("older still", 50L)
        assertEquals("new", store.latest())
    }

    @Test
    fun `only the most recent reports are kept`() {
        val store = store(keep = 2)
        listOf(1L, 2L, 3L, 4L).forEach { store.save("crash $it", it) }
        val files = File(folder.root, "crashes").listFiles().orEmpty().map { it.name }.sorted()
        assertEquals(listOf("crash-3.txt", "crash-4.txt"), files)
        assertEquals("crash 4", store.latest())
    }

    @Test
    fun `turning reporting off saves nothing`() {
        val store = store()
        store.enabled = false
        store.save("secret trace", 100L)
        assertNull(store.latest())
        assertTrue(File(folder.root, "crashes").listFiles().isNullOrEmpty())
    }

    @Test
    fun `clearing removes every report`() {
        val store = store()
        store.save("a", 1L)
        store.save("b", 2L)
        store.clear()
        assertNull(store.latest())
    }

    @Test
    fun `reports are ordered by time and not by file name text`() {
        val store = store()
        store.save("ten", 10L)
        store.save("nine", 9L)
        assertEquals("ten", store.latest())
    }
}

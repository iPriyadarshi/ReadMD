package com.readmd.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderSortTest {

    private fun entry(name: String, lastModified: Long = 0L) =
        FolderEntry(id = "id-$name", name = name, lastModified = lastModified)

    private val entries = listOf(
        entry("beta.md", lastModified = 100L),
        entry("Alpha.md", lastModified = 300L),
        entry("gamma.md", lastModified = 200L),
    )

    @Test
    fun `name ascending is case-insensitive`() {
        assertEquals(
            listOf("Alpha.md", "beta.md", "gamma.md"),
            FolderSort.NAME_ASC.apply(entries).map { it.name },
        )
    }

    @Test
    fun `name descending reverses the case-insensitive order`() {
        assertEquals(
            listOf("gamma.md", "beta.md", "Alpha.md"),
            FolderSort.NAME_DESC.apply(entries).map { it.name },
        )
    }

    @Test
    fun `newest first orders by last modified descending`() {
        assertEquals(
            listOf("Alpha.md", "gamma.md", "beta.md"),
            FolderSort.NEWEST.apply(entries).map { it.name },
        )
    }

    @Test
    fun `oldest first orders by last modified ascending`() {
        assertEquals(
            listOf("beta.md", "gamma.md", "Alpha.md"),
            FolderSort.OLDEST.apply(entries).map { it.name },
        )
    }

    @Test
    fun `unknown persisted id falls back to name ascending`() {
        assertEquals(FolderSort.NAME_ASC, FolderSort.fromId(null))
        assertEquals(FolderSort.NAME_ASC, FolderSort.fromId("GARBAGE"))
        assertEquals(FolderSort.NEWEST, FolderSort.fromId("NEWEST"))
    }

    @Test
    fun `sorting does not mutate the input list`() {
        val input = entries.toList()
        FolderSort.NEWEST.apply(input)
        assertEquals(entries, input)
    }
}

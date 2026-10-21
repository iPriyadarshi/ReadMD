package com.readmd.app.domain

enum class ColumnAlignment { START, CENTER, END }

data class TableData(
    val header: List<String>,
    val alignments: List<ColumnAlignment>,
    val rows: List<List<String>>,
)

/** A document with an "open wide table" link added before each wide table. */
data class TablesPrepared(
    val markdown: String,
    /** The wide tables, indexed by the number in their link. */
    val tables: List<TableData>,
)

/**
 * Finds GitHub-style pipe tables. Tables that are too wide to read once squeezed into the
 * page get a "View wide table" link above them that opens the full table in a viewer that
 * scrolls sideways. Narrow tables, and anything inside code, are left exactly as they are.
 */
object MarkdownTables {
    const val LINK_PREFIX = "readmd-table:"

    /** Tables with at least this many columns, or this many characters per row, count as wide. */
    const val WIDE_COLUMNS = 5
    const val WIDE_ROW_CHARS = 36

    private val DELIMITER_CELL = Regex("^:?-+:?$")
    private val OPENING_FENCE = Regex("^\\s*(`{3,}|~{3,})")
    private val IMAGE_OR_LINK = Regex("!?\\[([^\\]]*)]\\([^)]*\\)")
    private val EMPHASIS = Regex("(\\*\\*|__|~~|`)")

    fun tableIndex(link: String): Int? =
        if (link.startsWith(LINK_PREFIX)) link.removePrefix(LINK_PREFIX).toIntOrNull() else null

    fun isWide(table: TableData): Boolean {
        val columns = table.header.size
        if (columns >= WIDE_COLUMNS) return true
        val widest = (0 until columns).sumOf { column ->
            (listOf(table.header) + table.rows).maxOf { row -> row.getOrNull(column)?.length ?: 0 }
        }
        return widest > WIDE_ROW_CHARS
    }

    fun prepare(markdown: String): TablesPrepared {
        if ('|' !in markdown) return TablesPrepared(markdown, emptyList())

        val lines = markdown.split("\n")
        val out = ArrayList<String>(lines.size + 4)
        val tables = ArrayList<TableData>()
        var fenceChar = ' '
        var fenceLength = 0

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val bare = line.trimEnd('\r')

            if (fenceLength > 0) {
                if (bare.trim().let { it.length >= fenceLength && it.all { c -> c == fenceChar } }) fenceLength = 0
                out += line
                i++
                continue
            }
            val fence = OPENING_FENCE.find(bare)?.groupValues?.get(1)
            if (fence != null) {
                fenceChar = fence[0]
                fenceLength = fence.length
                out += line
                i++
                continue
            }

            val table = if (i + 1 < lines.size) parseTable(lines, i) else null
            if (table == null) {
                out += line
                i++
                continue
            }
            val (data, consumed) = table
            if (isWide(data)) {
                out += "[⤢ View wide table]($LINK_PREFIX${tables.size})"
                out += ""
                tables += data
            }
            for (k in i until i + consumed) out += lines[k]
            i += consumed
        }
        return TablesPrepared(out.joinToString("\n"), tables)
    }

    /** The table starting at [start] and how many lines it spans, or null if there isn't one. */
    private fun parseTable(lines: List<String>, start: Int): Pair<TableData, Int>? {
        val headerLine = lines[start].trimEnd('\r')
        if ('|' !in headerLine) return null
        if (headerLine.length - headerLine.trimStart().length > 3) return null
        if (headerLine.trimStart().let { it.startsWith(">") || it.startsWith("- ") || it.startsWith("* ") }) return null

        val header = splitRow(headerLine)
        val delimiter = splitRow(lines[start + 1].trimEnd('\r'))
        if (header.isEmpty() || header.size != delimiter.size) return null
        if (!delimiter.all { DELIMITER_CELL.matches(it) }) return null

        val rows = ArrayList<List<String>>()
        var next = start + 2
        while (next < lines.size) {
            val row = lines[next].trimEnd('\r')
            if (row.isBlank() || '|' !in row) break
            rows += splitRow(row)
            next++
        }
        val alignments = delimiter.map {
            when {
                it.startsWith(":") && it.endsWith(":") -> ColumnAlignment.CENTER
                it.endsWith(":") -> ColumnAlignment.END
                else -> ColumnAlignment.START
            }
        }
        val data = TableData(
            header = header.map(::plain),
            alignments = alignments,
            rows = rows.map { cells -> List(header.size) { column -> plain(cells.getOrNull(column).orEmpty()) } },
        )
        return data to (next - start)
    }

    /** Splits a table row into trimmed cells; `\|` stays inside its cell and outer pipes are optional. */
    fun splitRow(line: String): List<String> {
        val cells = ArrayList<String>()
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length && line[i + 1] == '|') {
                current.append('|')
                i += 2
                continue
            }
            if (c == '|') {
                cells += current.toString().trim()
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        cells += current.toString().trim()

        val trimmed = line.trim()
        if (trimmed.startsWith("|")) cells.removeAt(0)
        if (trimmed.endsWith("|") && !trimmed.endsWith("\\|") && cells.isNotEmpty()) cells.removeAt(cells.lastIndex)
        return cells
    }

    /** Cell text with the common inline markdown removed, for display in the viewer. */
    fun plain(cell: String): String =
        cell.replace(IMAGE_OR_LINK, "$1").replace(EMPHASIS, "").trim()
}

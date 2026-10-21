package com.readmd.app.domain

/** Turns the rendered document text into pieces a text-to-speech engine can read. */
object SpeechText {

    /** Engines refuse single utterances beyond a few thousand characters (the platform limit is 4000). */
    const val MAX_CHUNK = 3500

    private const val HELPER_LABEL = "⧉ Copy|⤢ Scroll|▶ View diagram|⤢ View wide table"
    private val HELPER_LINE = Regex("^\\s*(?:$HELPER_LABEL)(?:\\s+(?:$HELPER_LABEL))*\\s*$")
    private val SENTENCE_END = Regex("(?<=[.!?])\\s+")
    private val SPACES = Regex("[ \\t\\u00A0]+")

    /**
     * Cleans [rendered] for speaking: the app's own link lines ("Copy", "View diagram"...),
     * image placeholders and pictographs are dropped, whitespace is tidied, and the text is
     * grouped into chunks of at most [maxChunk] characters that never split a line unless
     * the line itself is too long (then at sentence ends, then at spaces).
     */
    fun chunks(rendered: String, maxChunk: Int = MAX_CHUNK): List<String> {
        val lines = rendered.lineSequence()
            .filterNot { HELPER_LINE.matches(it) }
            .map { clean(it) }
            .filter { it.isNotEmpty() }
            .flatMap { splitLong(it, maxChunk).asSequence() }
            .toList()

        val out = ArrayList<String>()
        val current = StringBuilder()
        for (line in lines) {
            if (current.isNotEmpty() && current.length + 1 + line.length > maxChunk) {
                out += current.toString()
                current.setLength(0)
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line)
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }

    private fun clean(line: String): String {
        val kept = StringBuilder(line.length)
        line.codePoints().forEach { cp ->
            val drop = cp == 0xFFFC || cp == 0xFE0F || cp == 0x200D ||
                Character.getType(cp) == Character.OTHER_SYMBOL.toInt()
            if (!drop) kept.appendCodePoint(cp)
        }
        return kept.toString().replace(SPACES, " ").trim()
    }

    private fun splitLong(line: String, max: Int): List<String> {
        if (line.length <= max) return listOf(line)
        val pieces = ArrayList<String>()
        val current = StringBuilder()
        for (sentence in line.split(SENTENCE_END)) {
            for (part in splitWords(sentence, max)) {
                if (current.isNotEmpty() && current.length + 1 + part.length > max) {
                    pieces += current.toString()
                    current.setLength(0)
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(part)
            }
        }
        if (current.isNotEmpty()) pieces += current.toString()
        return pieces
    }

    /** Splits text with no sentence ends at spaces; a single word longer than [max] is cut as is. */
    private fun splitWords(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val parts = ArrayList<String>()
        val current = StringBuilder()
        for (word in text.split(' ')) {
            if (word.length > max) {
                if (current.isNotEmpty()) {
                    parts += current.toString()
                    current.setLength(0)
                }
                parts += word.chunked(max)
                continue
            }
            if (current.isNotEmpty() && current.length + 1 + word.length > max) {
                parts += current.toString()
                current.setLength(0)
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) parts += current.toString()
        return parts
    }
}

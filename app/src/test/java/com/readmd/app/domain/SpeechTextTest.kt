package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    @Test
    fun `blank text gives no chunks`() {
        assertTrue(SpeechText.chunks("").isEmpty())
        assertTrue(SpeechText.chunks("  \n\n \t ").isEmpty())
    }

    @Test
    fun `short documents become one chunk with one line per paragraph`() {
        assertEquals(listOf("Title\nFirst paragraph.\nSecond one."), SpeechText.chunks("Title\n\nFirst paragraph.\n\nSecond one."))
    }

    @Test
    fun `the apps own link lines are not read`() {
        val text = "Intro\n⧉ Copy   ⤢ Scroll\ncode here\n▶ View diagram\n⤢ View wide table\nOutro"
        assertEquals(listOf("Intro\ncode here\nOutro"), SpeechText.chunks(text))
    }

    @Test
    fun `placeholders and pictographs are removed and spaces tidied`() {
        assertEquals(listOf("Launch day now"), SpeechText.chunks("Launch ￼ 🚀  day  ⚠️ now"))
        assertEquals(listOf("Fact 1"), SpeechText.chunks("Fact\t 1"))
    }

    @Test
    fun `lines are grouped up to the limit without being split`() {
        val chunks = SpeechText.chunks("aaaa\nbbbb\ncccc\ndddd", maxChunk = 9)
        assertEquals(listOf("aaaa\nbbbb", "cccc\ndddd"), chunks)
    }

    @Test
    fun `an overlong line is split at sentence ends`() {
        val line = "One two three. Four five six. Seven eight nine."
        val chunks = SpeechText.chunks(line, maxChunk = 30)
        assertEquals(listOf("One two three. Four five six.", "Seven eight nine."), chunks)
        assertTrue(chunks.all { it.length <= 30 })
    }

    @Test
    fun `text without sentence ends is split at spaces`() {
        val chunks = SpeechText.chunks("alpha beta gamma delta epsilon", maxChunk = 12)
        assertEquals(listOf("alpha beta", "gamma delta", "epsilon"), chunks)
    }

    @Test
    fun `a single word longer than the limit is cut`() {
        assertEquals(listOf("abcd", "efgh", "ij"), SpeechText.chunks("abcdefghij", maxChunk = 4))
    }

    @Test
    fun `no chunk ever exceeds the limit`() {
        val text = (1..400).joinToString(" ") { "word$it" } + ". " + (1..400).joinToString(" ") { "more$it" }
        assertTrue(SpeechText.chunks(text, maxChunk = 200).all { it.length <= 200 })
    }
}

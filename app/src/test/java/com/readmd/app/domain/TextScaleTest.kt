package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextScaleTest {

    @Test
    fun `step moves by ten percent`() {
        assertEquals(110, TextScale.step(100, 1))
        assertEquals(90, TextScale.step(100, -1))
    }

    @Test
    fun `step never goes below the minimum`() {
        assertEquals(TextScale.MIN, TextScale.step(TextScale.MIN, -1))
    }

    @Test
    fun `step never goes above the maximum`() {
        assertEquals(TextScale.MAX, TextScale.step(TextScale.MAX, 1))
    }

    @Test
    fun `clamp restricts stored values to the supported range`() {
        assertEquals(TextScale.MIN, TextScale.clamp(10))
        assertEquals(TextScale.MAX, TextScale.clamp(999))
        assertEquals(120, TextScale.clamp(120))
    }
}

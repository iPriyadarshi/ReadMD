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
    fun `scaled applies the pinch factor and rounds`() {
        assertEquals(150, TextScale.scaled(100, 1.5f))
        assertEquals(113, TextScale.scaled(100, 1.125f))
    }

    @Test
    fun `scaled stays within bounds`() {
        assertEquals(TextScale.MAX, TextScale.scaled(150, 5f))
        assertEquals(TextScale.MIN, TextScale.scaled(100, 0.1f))
    }

    @Test
    fun `clamp restricts stored values to the supported range`() {
        assertEquals(TextScale.MIN, TextScale.clamp(10))
        assertEquals(TextScale.MAX, TextScale.clamp(999))
        assertEquals(120, TextScale.clamp(120))
    }
}

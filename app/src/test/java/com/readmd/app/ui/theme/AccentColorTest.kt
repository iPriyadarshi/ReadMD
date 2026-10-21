package com.readmd.app.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {

    @Test
    fun `ids round-trip`() {
        AccentColor.entries.forEach { assertEquals(it, AccentColor.fromId(it.id)) }
    }

    @Test
    fun `unknown or missing id falls back to default`() {
        assertEquals(AccentColor.DEFAULT, AccentColor.fromId("magenta"))
        assertEquals(AccentColor.DEFAULT, AccentColor.fromId(null))
    }

    @Test
    fun `dark swatches are lighter than light swatches`() {
        AccentColor.entries.forEach {
            assertTrue(it.label, it.swatch(dark = true).luminance() > it.swatch(dark = false).luminance())
        }
    }
}

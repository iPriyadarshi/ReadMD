package com.readmd.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `saved ordinals keep their meaning`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromOrdinal(0))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromOrdinal(1))
        assertEquals(ThemeMode.DARK, ThemeMode.fromOrdinal(2))
        assertEquals(ThemeMode.SEPIA, ThemeMode.fromOrdinal(3))
        assertEquals(ThemeMode.BLACK, ThemeMode.fromOrdinal(4))
    }

    @Test
    fun `unknown ordinal falls back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromOrdinal(99))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromOrdinal(-1))
    }
}

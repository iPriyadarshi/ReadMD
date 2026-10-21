package com.readmd.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * User-selectable accent. [DEFAULT] keeps the standard behaviour (dynamic color on
 * Android 12+, the app's blue elsewhere); the others override the primary color in
 * the light, dark and true-black themes. Sepia keeps its own fixed palette.
 */
enum class AccentColor(
    val id: String,
    val label: String,
    private val light: Color,
    private val dark: Color,
) {
    DEFAULT("default", "Default", Color(0xFF1B5E9C), Color(0xFF90CAF9)),
    TEAL("teal", "Teal", Color(0xFF00796B), Color(0xFF4DB6AC)),
    GREEN("green", "Green", Color(0xFF2E7D32), Color(0xFF81C784)),
    ORANGE("orange", "Orange", Color(0xFFE65100), Color(0xFFFFB74D)),
    RED("red", "Red", Color(0xFFC62828), Color(0xFFEF9A9A)),
    PURPLE("purple", "Purple", Color(0xFF6A1B9A), Color(0xFFCE93D8));

    /** The swatch shown in the picker. */
    fun swatch(dark: Boolean): Color = if (dark) this.dark else light

    companion object {
        fun fromId(id: String?): AccentColor = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

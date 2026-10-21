package com.readmd.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.readmd.app.R

/**
 * User-selectable accent. [DEFAULT] keeps the standard behaviour (dynamic color on
 * Android 12+, the app's blue elsewhere); the others override the primary color in
 * the light, dark and true-black themes. Sepia keeps its own fixed palette.
 */
enum class AccentColor(
    val id: String,
    @StringRes val labelRes: Int,
    private val light: Color,
    private val dark: Color,
) {
    DEFAULT("default", R.string.accent_default, Color(0xFF1B5E9C), Color(0xFF90CAF9)),
    TEAL("teal", R.string.accent_teal, Color(0xFF00796B), Color(0xFF4DB6AC)),
    GREEN("green", R.string.accent_green, Color(0xFF2E7D32), Color(0xFF81C784)),
    ORANGE("orange", R.string.accent_orange, Color(0xFFE65100), Color(0xFFFFB74D)),
    RED("red", R.string.accent_red, Color(0xFFC62828), Color(0xFFEF9A9A)),
    PURPLE("purple", R.string.accent_purple, Color(0xFF6A1B9A), Color(0xFFCE93D8));

    /** The swatch shown in the picker. */
    fun swatch(dark: Boolean): Color = if (dark) this.dark else light

    companion object {
        fun fromId(id: String?): AccentColor = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

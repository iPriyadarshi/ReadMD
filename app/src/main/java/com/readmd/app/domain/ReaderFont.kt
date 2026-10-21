package com.readmd.app.domain

import androidx.annotation.StringRes
import com.readmd.app.R

/** Body font family choices for the rendered reader view. */
enum class ReaderFont(val id: String, @StringRes val labelRes: Int) {
    SANS("sans", R.string.font_sans),
    SERIF("serif", R.string.font_serif),
    MONO("mono", R.string.font_mono);

    companion object {
        val DEFAULT = SANS

        fun fromId(id: String?): ReaderFont = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

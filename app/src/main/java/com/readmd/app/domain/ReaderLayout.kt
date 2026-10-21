package com.readmd.app.domain

import androidx.annotation.StringRes
import com.readmd.app.R

/** Line spacing presets for the rendered reader view; [multiplier] scales the font's line height. */
enum class LineSpacing(val id: String, @StringRes val labelRes: Int, val multiplier: Float) {
    COMPACT("compact", R.string.spacing_compact, 1.0f),
    NORMAL("normal", R.string.spacing_normal, 1.2f),
    RELAXED("relaxed", R.string.spacing_relaxed, 1.5f);

    companion object {
        val DEFAULT = NORMAL

        fun fromId(id: String?): LineSpacing = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** Horizontal page margin presets for the rendered and raw reader views. */
enum class PageMargin(val id: String, @StringRes val labelRes: Int, val dp: Int) {
    NARROW("narrow", R.string.margin_narrow, 8),
    NORMAL("normal", R.string.margin_normal, 16),
    WIDE("wide", R.string.margin_wide, 32);

    companion object {
        val DEFAULT = NORMAL

        fun fromId(id: String?): PageMargin = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

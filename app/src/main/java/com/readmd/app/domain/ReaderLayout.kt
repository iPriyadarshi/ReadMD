package com.readmd.app.domain

/** Line spacing presets for the rendered reader view; [multiplier] scales the font's line height. */
enum class LineSpacing(val id: String, val label: String, val multiplier: Float) {
    COMPACT("compact", "Compact", 1.0f),
    NORMAL("normal", "Normal", 1.2f),
    RELAXED("relaxed", "Relaxed", 1.5f);

    companion object {
        val DEFAULT = NORMAL

        fun fromId(id: String?): LineSpacing = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** Horizontal page margin presets for the rendered and raw reader views. */
enum class PageMargin(val id: String, val label: String, val dp: Int) {
    NARROW("narrow", "Narrow", 8),
    NORMAL("normal", "Normal", 16),
    WIDE("wide", "Wide", 32);

    companion object {
        val DEFAULT = NORMAL

        fun fromId(id: String?): PageMargin = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

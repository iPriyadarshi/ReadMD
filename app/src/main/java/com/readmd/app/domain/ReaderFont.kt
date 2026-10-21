package com.readmd.app.domain

/** Body font family choices for the rendered reader view. */
enum class ReaderFont(val id: String, val label: String) {
    SANS("sans", "Sans-serif"),
    SERIF("serif", "Serif"),
    MONO("mono", "Monospace");

    companion object {
        val DEFAULT = SANS

        fun fromId(id: String?): ReaderFont = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

package com.readmd.app.domain

/** Screen-size decisions for the reader layout. */
object WindowLayout {
    /**
     * From this width on, the table of contents is a permanent side pane. It covers tablets
     * and phones in landscape, and leaves phones in portrait with the sliding drawer.
     */
    const val WIDE_DP = 720

    fun isWide(screenWidthDp: Int): Boolean = screenWidthDp >= WIDE_DP
}

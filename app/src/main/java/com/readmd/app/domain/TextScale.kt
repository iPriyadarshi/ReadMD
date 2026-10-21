package com.readmd.app.domain

import kotlin.math.roundToInt

/** Reader text size as a percentage of the default size, adjusted in fixed steps. */
object TextScale {
    const val MIN = 80
    const val MAX = 200
    const val STEP = 10
    const val DEFAULT = 100

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)

    /** Applies a pinch [zoom] factor to [current], staying within bounds. */
    fun scaled(current: Int, zoom: Float): Int = clamp((current * zoom).roundToInt())

    /** Moves [current] by [steps] increments (negative to shrink), staying within bounds. */
    fun step(current: Int, steps: Int): Int = clamp(current + steps * STEP)
}

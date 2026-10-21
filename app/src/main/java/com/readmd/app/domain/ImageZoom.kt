package com.readmd.app.domain

/** Zoom and pan limits for the fullscreen image viewer, kept free of UI types for testing. */
object ImageZoom {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 8f
    const val DOUBLE_TAP_SCALE = 3f

    /** Largest side, in pixels, an image is decoded at; bigger images are sub-sampled. */
    const val MAX_DECODE_SIZE = 4096

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /** New scale after a pinch gesture of [zoom] (a factor, 1 = no change). */
    fun scaleBy(scale: Float, zoom: Float): Float = clampScale(scale * zoom)

    /**
     * How far the image may be dragged along one axis: the part of the scaled image
     * that overflows the viewport, split between both sides.
     */
    fun maxPan(viewport: Float, scale: Float): Float = (viewport * (scale - 1f) / 2f).coerceAtLeast(0f)

    fun clampOffset(offset: Float, viewport: Float, scale: Float): Float {
        val limit = maxPan(viewport, scale)
        return offset.coerceIn(-limit, limit)
    }

    /** Zooms back out when already zoomed in, otherwise zooms in. */
    fun doubleTapTarget(scale: Float): Float = if (scale > MIN_SCALE + 0.01f) MIN_SCALE else DOUBLE_TAP_SCALE

    /** Power-of-two subsampling factor that brings the larger side down to [maxSize] or less. */
    fun sampleSize(width: Int, height: Int, maxSize: Int = MAX_DECODE_SIZE): Int {
        var sample = 1
        var largest = maxOf(width, height)
        while (largest > maxSize) {
            sample *= 2
            largest /= 2
        }
        return sample
    }
}

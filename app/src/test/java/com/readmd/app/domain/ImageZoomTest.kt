package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageZoomTest {

    @Test
    fun `scale stays within the allowed range`() {
        assertEquals(1f, ImageZoom.clampScale(0.2f), 0f)
        assertEquals(8f, ImageZoom.clampScale(50f), 0f)
        assertEquals(2.5f, ImageZoom.clampScale(2.5f), 0f)
    }

    @Test
    fun `pinch factors multiply the current scale and are clamped`() {
        assertEquals(3f, ImageZoom.scaleBy(2f, 1.5f), 0.0001f)
        assertEquals(1f, ImageZoom.scaleBy(1.2f, 0.1f), 0f)
        assertEquals(8f, ImageZoom.scaleBy(6f, 3f), 0f)
    }

    @Test
    fun `an unzoomed image cannot be panned`() {
        assertEquals(0f, ImageZoom.maxPan(1000f, 1f), 0f)
        assertEquals(0f, ImageZoom.clampOffset(120f, 1000f, 1f), 0f)
    }

    @Test
    fun `panning is limited to the overflow on each side`() {
        assertEquals(500f, ImageZoom.maxPan(1000f, 2f), 0f)
        assertEquals(500f, ImageZoom.clampOffset(900f, 1000f, 2f), 0f)
        assertEquals(-500f, ImageZoom.clampOffset(-900f, 1000f, 2f), 0f)
        assertEquals(120f, ImageZoom.clampOffset(120f, 1000f, 2f), 0f)
    }

    @Test
    fun `double tap toggles between fit and zoomed in`() {
        assertEquals(ImageZoom.DOUBLE_TAP_SCALE, ImageZoom.doubleTapTarget(1f), 0f)
        assertEquals(1f, ImageZoom.doubleTapTarget(3f), 0f)
        assertEquals(1f, ImageZoom.doubleTapTarget(1.5f), 0f)
    }

    @Test
    fun `sample size halves until the image fits`() {
        assertEquals(1, ImageZoom.sampleSize(800, 600))
        assertEquals(1, ImageZoom.sampleSize(4096, 100))
        assertEquals(2, ImageZoom.sampleSize(5000, 3000))
        assertEquals(4, ImageZoom.sampleSize(9000, 2000))
        assertEquals(1, ImageZoom.sampleSize(10, 10))
    }
}

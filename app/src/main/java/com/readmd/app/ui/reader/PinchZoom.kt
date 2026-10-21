package com.readmd.app.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Reports two-finger pinch zoom factors (per event, relative to the previous one)
 * without interfering with single-finger scrolling or text selection. Pinch events
 * are watched on the initial pass and consumed so children don't also react.
 * [onEnd] runs once all fingers lift after at least one zoom was reported.
 */
fun Modifier.pinchToZoom(
    onZoom: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var zoomed = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    zoomed = true
                    onZoom(zoom)
                    event.changes.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
        if (zoomed) onEnd()
    }
}

package com.example.gesturestest.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlin.math.abs

fun Modifier.customTransformGestures(
    onGesture: (offset: IntOffset, zoom: Float) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var zoom = 1f
        var offset = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val zoomChange = event.calculateZoom()
            val offsetChange = event.calculatePan()
            if (!pastTouchSlop) {
                zoom *= zoomChange
                offset += offsetChange
                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - zoom) * centroidSize
                val offsetMotion = offset.getDistance()
                pastTouchSlop = (zoomMotion > touchSlop || offsetMotion > touchSlop)
            }
            if (pastTouchSlop && (zoomChange != 1f || offsetChange != Offset.Zero)) {
                onGesture(offsetChange.round(), zoomChange)
            }
        } while (event.changes.any { it.pressed })
    }
}

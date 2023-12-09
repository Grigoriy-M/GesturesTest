package com.example.gesturestest

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal fun IntRect.factorToOverlaps(other: IntRect): Float = when {
    aspectRatio() > other.aspectRatio() -> width.absoluteValue.toFloat() / other.width.absoluteValue
    else -> height.absoluteValue.toFloat() / other.height.absoluteValue
}

internal fun IntRect.aspectRatio(): Float = width.absoluteValue.toFloat() / height.absoluteValue

internal fun IntRect.times(factor: Float) = IntRect(
    left = (left * factor).roundToInt(),
    top = (top * factor).roundToInt(),
    right = (right * factor).roundToInt(),
    bottom = (bottom * factor).roundToInt(),
)

@Stable
fun IntRect.transform(scale: Float, offset: IntOffset): IntRect {
    val radiusX = (scale * width.absoluteValue / 2).roundToInt()
    val radiusY = (scale * height.absoluteValue / 2).roundToInt()
    return copy(
        left = offset.x - radiusX,
        top = offset.y - radiusY,
        right = offset.x + radiusX,
        bottom = offset.y + radiusY,
    )
}

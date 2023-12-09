package com.example.gesturestest

import androidx.compose.ui.unit.IntRect
import com.example.gesturestest.TransformFilter.FULL_SCALE
import com.example.gesturestest.TransformFilter.OFFSET_IN_PARENT_RECT
import com.example.gesturestest.TransformFilter.SCALE_TO_PARENT
import com.example.gesturestest.util.toLog
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

enum class TransformFilter {
    FULL_SCALE,
    SCALE_TO_PARENT,
    OFFSET_IN_PARENT_RECT
}

private const val MAX_ZOOM = 8.05f
private const val MIN_ZOOM = 0.5f

object TransformFilterDelegate {

    private val filters = LinkedHashSet<TransformFilter>()

    fun addFilter(filter: TransformFilter?): TransformFilterDelegate {
        filter?.let(filters::add)
        return this
    }

    fun release(
        paren: IntRect,
        child: IntRect,
        predictState: TransformState,
    ): TransformState = filters
        .fold(predictState) { newState, filter -> applyFilter(newState, filter, paren, child) }
        .run { copy(offset = offset - child.center) }
        .also { filters.clear() }

    private fun applyFilter(
        newState: TransformState,
        filter: TransformFilter,
        paren: IntRect,
        child: IntRect,
    ) = when (filter) {
        FULL_SCALE -> newState.fullScale()
        SCALE_TO_PARENT -> newState.scaleToParent(paren = paren, child = child)
        OFFSET_IN_PARENT_RECT -> newState.offsetInParent(paren = paren, child = child)
    }

    private fun TransformState.fullScale() = copy(scale = min(max(MIN_ZOOM, scale), MAX_ZOOM))

    private fun TransformState.scaleToParent(paren: IntRect, child: IntRect): TransformState {
        val factor = paren.factorToOverlaps(child.transform(scale, offset))
        return if (factor < 1f) copy(scale = scale * factor) else this
    }

    private fun TransformState.offsetInParent(
        paren: IntRect,
        child: IntRect,
    ): TransformState {
        val predictChild = child.transform(scale, offset)
        var tempOffset = offset
        if (paren.height.absoluteValue >= predictChild.height.absoluteValue) {
            if (paren.top >= predictChild.top) {
                tempOffset = tempOffset.copy(y = tempOffset.y + (paren.top - predictChild.top))
            }
            if (paren.bottom <= predictChild.bottom) {
                tempOffset = tempOffset.copy(y = tempOffset.y - (predictChild.bottom - paren.bottom))
            }
        }
        if (paren.width.absoluteValue >= predictChild.width.absoluteValue) {
            if (paren.left >= predictChild.left) {
                tempOffset = tempOffset.copy(x = tempOffset.x + ((paren.left - predictChild.left)))
            }
            if (paren.right <= predictChild.right) {
                tempOffset = tempOffset.copy(x = tempOffset.x - (predictChild.right - paren.right))
            }
        }
        if (paren.height.absoluteValue < predictChild.height.absoluteValue) {
            if (paren.top < predictChild.top) {
                tempOffset = tempOffset.copy(y = tempOffset.y - (predictChild.top - paren.top))
            }
            if (predictChild.bottom < paren.bottom) {
                tempOffset = tempOffset.copy(y = tempOffset.y + (paren.bottom - predictChild.bottom))
            }
        }
        if (paren.width.absoluteValue < predictChild.width.absoluteValue) {
            if (paren.left < predictChild.left) {
                tempOffset = tempOffset.copy(x = tempOffset.x + ((paren.left - predictChild.left)))
            }
            if (paren.right > predictChild.right) {
                tempOffset = tempOffset.copy(x = tempOffset.x + (paren.right - predictChild.right))
            }
        }
        "paren:$paren predictChild:$predictChild".toLog()
        return copy(offset = tempOffset)
    }
}

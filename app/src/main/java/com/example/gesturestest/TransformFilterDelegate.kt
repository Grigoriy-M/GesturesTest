package com.example.gesturestest

import androidx.compose.ui.unit.IntRect
import com.example.gesturestest.TransformFilterDelegate.TransformFilter
import com.example.gesturestest.TransformFilterDelegate.TransformFilter.FULL_SCALE
import com.example.gesturestest.TransformFilterDelegate.TransformFilter.OFFSET_IN_PARENT_RECT
import com.example.gesturestest.TransformFilterDelegate.TransformFilter.SCALE_TO_PARENT
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

internal interface TransformFilterDelegate {
    fun addFilter(filter: TransformFilter): TransformFilterDelegate
    fun release(parentRect: IntRect, childRect: IntRect, predictState: TransformState): TransformState

    enum class TransformFilter {
        FULL_SCALE,
        SCALE_TO_PARENT,
        OFFSET_IN_PARENT_RECT
    }
}

private const val MAX_ZOOM = 8.05f
private const val MIN_ZOOM = 0.5f

internal class TransformFilterDelegateImpl : TransformFilterDelegate {

    private var parent: IntRect = IntRect.Zero
    private var child: IntRect = IntRect.Zero
    private val filters = LinkedHashSet<TransformFilter>()

    override fun addFilter(filter: TransformFilter): TransformFilterDelegate {
        filters.add(filter)
        return this
    }

    override fun release(
        parentRect: IntRect,
        childRect: IntRect,
        predictState: TransformState
    ): TransformState {
        parent = parentRect
        child = childRect
        return filters
            .fold(predictState) { state, filter -> applyFilter(state, filter) }
            .run { copy(offset = offset - child.center) }
            .also { filters.clear() }
    }

    private fun applyFilter(
        state: TransformState,
        filter: TransformFilter,
    ) = when (filter) {
        FULL_SCALE -> state.fullScale()
        SCALE_TO_PARENT -> state.scaleToParent()
        OFFSET_IN_PARENT_RECT -> state.offsetInParent()
    }

    private fun TransformState.fullScale() = copy(scale = min(max(MIN_ZOOM, scale), MAX_ZOOM))

    private fun TransformState.scaleToParent(): TransformState {
        val factor = parent.factorToOverlaps(child.transform(scale, offset))
        return if (factor < 1f) copy(scale = scale * factor) else this
    }

    private fun TransformState.offsetInParent(): TransformState {
        val predictChild = child.transform(scale, offset)
        var tempOffset = offset
        if (parent.height.absoluteValue >= predictChild.height.absoluteValue) {
            if (parent.top >= predictChild.top) {
                tempOffset = tempOffset.copy(y = tempOffset.y + (parent.top - predictChild.top))
            }
            if (parent.bottom <= predictChild.bottom) {
                tempOffset = tempOffset.copy(y = tempOffset.y - (predictChild.bottom - parent.bottom))
            }
        } else {
            if (parent.top < predictChild.top) {
                tempOffset = tempOffset.copy(y = tempOffset.y - (predictChild.top - parent.top))
            }
            if (predictChild.bottom < parent.bottom) {
                tempOffset = tempOffset.copy(y = tempOffset.y + (parent.bottom - predictChild.bottom))
            }
        }
        if (parent.width.absoluteValue >= predictChild.width.absoluteValue) {
            if (parent.left >= predictChild.left) {
                tempOffset = tempOffset.copy(x = tempOffset.x + ((parent.left - predictChild.left)))
            }
            if (parent.right <= predictChild.right) {
                tempOffset = tempOffset.copy(x = tempOffset.x - (predictChild.right - parent.right))
            }
        } else {
            if (parent.left < predictChild.left) {
                tempOffset = tempOffset.copy(x = tempOffset.x + ((parent.left - predictChild.left)))
            }
            if (parent.right > predictChild.right) {
                tempOffset = tempOffset.copy(x = tempOffset.x + (parent.right - predictChild.right))
            }
        }
        return copy(offset = tempOffset)
    }
}

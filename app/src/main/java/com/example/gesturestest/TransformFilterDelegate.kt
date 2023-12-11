package com.example.gesturestest

import android.animation.ValueAnimator
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

internal interface TransformFilterDelegate {

    val transformState: StateFlow<TransformState>
    fun preprocessingFilter(offset: IntOffset, zoom: Float, parent: IntRect, child: IntRect)
    fun postprocessingFilter(parent: IntRect, child: IntRect)
}

private const val MAX_ZOOM = 8.05f
private const val MIN_ZOOM = 0.9f
private const val MIN_STEP_TO_ZOOM_FACTOR = 1.5f
private const val MAX_STEP_TO_ZOOM_FACTOR = 8.0f

private const val BORDERLESS_MODE_FACTOR = 1.25f
private const val BORDERLESS_MIN_FACTOR = 1.1f
private const val BORDERLESS_MAX_FACTOR = 1.3f

private const val ANIMATION_DURATION = 200L

internal class TransformFilterDelegateImpl : TransformFilterDelegate {

    private var parent: IntRect = IntRect.Zero
    private var child: IntRect = IntRect.Zero
    private var zoom: Float = 1f
    private val _transformState = MutableStateFlow(TransformState(scale = 1.0f, offset = IntOffset.Zero))
    override val transformState: StateFlow<TransformState> = _transformState.asStateFlow()

    private val valueAnimator = ValueAnimator()

    override fun preprocessingFilter(
        offset: IntOffset,
        zoom: Float,
        parent: IntRect,
        child: IntRect,
    ) {
        this.parent = parent
        this.child = child
        this.zoom = zoom
        _transformState.update { state ->
            state
                .predictState(offset, zoom)
                .includeChildOffset()
                .fullScale()
                .scaleToBorderlessMode()
                .offsetRelativeToParent()
                .excludeChildOffset()
        }
    }

    override fun postprocessingFilter(
        parent: IntRect,
        child: IntRect,
    ) {
        this.parent = parent
        this.child = child
        valueAnimator.removeAllUpdateListeners()
        valueAnimator.cancel()
        val scale = _transformState.value.scale
        val factor = parent.factorToOverlaps(child)
        valueAnimator.apply {
            duration = ANIMATION_DURATION
            when {
                scale > MAX_STEP_TO_ZOOM_FACTOR -> {
                    setFloatValues(scale, MAX_ZOOM)
                    addUpdateListener(::animateToMaxZoom)
                }
                scale in factor..MIN_STEP_TO_ZOOM_FACTOR -> {
                    setFloatValues(scale, factor)
                    addUpdateListener(::animateToBorderlessMode)
                }
                scale in 1f..factor -> {
                    setFloatValues(scale, factor)
                    addUpdateListener(::animateToBorderlessMode)
                }
                scale < 1f -> {
                    setFloatValues(scale, 1f)
                    addUpdateListener(::animateToBorderlessMode)
                }
            }
            start()
        }
    }

    private fun animateToBorderlessMode(valueAnimator: ValueAnimator) {
        val scale = (valueAnimator.animatedValue as? Float) ?: return
        _transformState.update { state ->
            state.copy(scale = scale)
                .includeChildOffset()
                .offsetRelativeToParent()
                .excludeChildOffset()
        }
    }

    private fun animateToMaxZoom(valueAnimator: ValueAnimator) {
        val scale = (valueAnimator.animatedValue as? Float) ?: return
        _transformState.update { state ->
            state.copy(scale = scale)
                .includeChildOffset()
                .offsetRelativeToParent()
                .excludeChildOffset()
        }
    }

    private fun TransformState.fullScale() = copy(scale = min(max(MIN_ZOOM, scale), MAX_ZOOM))

    private fun TransformState.scaleToBorderlessMode(): TransformState {
        val factor = parent.factorToOverlaps(child)
        val isBorderlessEnable = factor <= BORDERLESS_MODE_FACTOR
        return when {
            isBorderlessEnable && zoom > 1f && scale in BORDERLESS_MIN_FACTOR..factor -> copy(scale = factor)
            isBorderlessEnable && zoom < 1f && scale in 1f..BORDERLESS_MIN_FACTOR -> copy(scale = 1f)
            else -> this
        }
    }

    private fun TransformState.offsetRelativeToParent(): TransformState {
        val predictChild = child.transform(scale, offset)
        var tempOffset = offset
        if (parent.height.absoluteValue <= predictChild.height.absoluteValue) {
            if (parent.top < predictChild.top) {
                tempOffset = tempOffset.copy(y = tempOffset.y - (predictChild.top - parent.top))
            }
            if (predictChild.bottom < parent.bottom) {
                tempOffset = tempOffset.copy(y = tempOffset.y + (parent.bottom - predictChild.bottom))
            }
        } else {
            tempOffset = tempOffset.copy(y = child.center.y)
        }
        if (parent.width.absoluteValue <= predictChild.width.absoluteValue) {
            if (parent.left < predictChild.left) {
                tempOffset = tempOffset.copy(x = tempOffset.x + (parent.left - predictChild.left))
            }
            if (parent.right > predictChild.right) {
                tempOffset = tempOffset.copy(x = tempOffset.x + (parent.right - predictChild.right))
            }
        } else {
            tempOffset = tempOffset.copy(x = child.center.x)
        }
        return copy(offset = tempOffset)
    }

    private fun TransformState.includeChildOffset() = copy(offset = offset + child.center)

    private fun TransformState.excludeChildOffset() = copy(offset = offset - child.center)

    private fun TransformState.predictState(
        offset: IntOffset,
        zoom: Float,
    ): TransformState = copy(
        scale = scale * zoom,
        offset = this.offset + offset,
    )
}

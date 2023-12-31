package com.example.gesturestest

import android.animation.ValueAnimator
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal interface TransformFilterDelegate {

    val transformState: StateFlow<TransformState>

    fun onParentUpdate(layoutCoordinates: LayoutCoordinates)
    fun onChildUpdate(layoutCoordinates: LayoutCoordinates)
    fun preprocessingFilter(offset: IntOffset, zoom: Float)
    fun postprocessingFilter()
}

private const val MAX_ZOOM = 8.05f
private const val MIN_ZOOM = 0.9f
private const val MIN_STEP_TO_ZOOM_FACTOR = 1.3f
private const val MAX_STEP_TO_ZOOM_FACTOR = 8.0f

private const val BORDERLESS_MODE_FACTOR = 1.25f
private const val BORDERLESS_MIN_FACTOR = 1.1f

private const val ANIMATION_DURATION = 200L

internal class TransformFilterDelegateImpl : TransformFilterDelegate {

    private var parent: IntRect = IntRect.Zero
    private var child: IntRect = IntRect.Zero
    private val _transformState = MutableStateFlow(TransformState(scale = 1.0f, offset = IntOffset.Zero))
    override val transformState: StateFlow<TransformState> = _transformState.asStateFlow()

    private val valueAnimator = ValueAnimator()
    private val factor: Float
        get() = parent.factorToOverlaps(child)
    private val isBorderlessEnable: Boolean
        get() = factor <= BORDERLESS_MODE_FACTOR

    override fun onParentUpdate(layoutCoordinates: LayoutCoordinates) {
        parent = layoutCoordinates.boundsInRoot().roundToIntRect()
    }

    override fun onChildUpdate(layoutCoordinates: LayoutCoordinates) {
        child = layoutCoordinates.boundsInRoot().roundToIntRect()
    }

    override fun preprocessingFilter(offset: IntOffset, zoom: Float) {
        _transformState.update { state ->
            state
                .predictState(offset, zoom)
                .includeChildOffset()
                .fullScale()
                .scaleToBorderlessMode(zoom)
                .offsetRelativeToParent()
                .excludeChildOffset()
        }
    }

    override fun postprocessingFilter() {
        val scale = _transformState.value.scale
        when {
            scale > MAX_STEP_TO_ZOOM_FACTOR -> animate(scale, MAX_STEP_TO_ZOOM_FACTOR)
            isBorderlessEnable && scale in BORDERLESS_MIN_FACTOR..MIN_STEP_TO_ZOOM_FACTOR -> {
                animate(scale, factor)
            }
            isBorderlessEnable && scale < BORDERLESS_MIN_FACTOR -> animate(scale, 1f)
            scale < MIN_STEP_TO_ZOOM_FACTOR -> animate(scale, 1f)
        }
    }

    private fun TransformState.fullScale() = copy(scale = min(max(MIN_ZOOM, scale), MAX_ZOOM))

    private fun TransformState.scaleToBorderlessMode(zoom: Float): TransformState {
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

    private fun animate(begin: Float, end: Float) {
        valueAnimator.apply {
            removeAllUpdateListeners()
            cancel()
            setFloatValues(begin, end)
            duration = ANIMATION_DURATION
            addUpdateListener { valueAnimator ->
                (valueAnimator.animatedValue as? Float)?.let { scale ->
                    _transformState.update { state ->
                        state.copy(scale = scale)
                            .includeChildOffset()
                            .offsetRelativeToParent()
                            .excludeChildOffset()
                    }
                }
            }
            start()
        }
    }

    private fun IntRect.factorToOverlaps(other: IntRect): Float = when {
        aspectRatio() > other.aspectRatio() -> width.absoluteValue.toFloat() / other.width.absoluteValue
        else -> height.absoluteValue.toFloat() / other.height.absoluteValue
    }

    private fun IntRect.aspectRatio(): Float = width.absoluteValue.toFloat() / height.absoluteValue

    private fun IntRect.transform(scale: Float, offset: IntOffset): IntRect {
        val radiusX = (scale * width.absoluteValue / 2).roundToInt()
        val radiusY = (scale * height.absoluteValue / 2).roundToInt()
        return copy(
            left = offset.x - radiusX,
            top = offset.y - radiusY,
            right = offset.x + radiusX,
            bottom = offset.y + radiusY,
        )
    }
}

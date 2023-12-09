package com.example.gesturestest

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import com.example.gesturestest.TransformFilter.FULL_SCALE
import com.example.gesturestest.TransformFilter.OFFSET_IN_PARENT_RECT
import com.example.gesturestest.TransformFilter.SCALE_TO_PARENT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ViewModel {
    val transformState: StateFlow<TransformState>
    fun onTransformUpdate(offset: IntOffset, zoom: Float)
    fun onChildLayoutCoordinatesUpdate(layoutCoordinates: LayoutCoordinates)
    fun onParenLayoutCoordinatesUpdate(layoutCoordinates: LayoutCoordinates)
}

class ViewModelImpl : ViewModel {

    private val _transformState = MutableStateFlow(TransformState(scale = 1.0f, offset = IntOffset.Zero))
    private var paren: IntRect = IntRect.Zero
    private var child: IntRect = IntRect.Zero

    override val transformState: StateFlow<TransformState> = _transformState.asStateFlow()

    override fun onTransformUpdate(offset: IntOffset, zoom: Float) {
        _transformState.update { state ->
            TransformFilterDelegate
                .addFilter(FULL_SCALE)
//                .addFilter(SCALE_TO_PARENT)
                .addFilter(OFFSET_IN_PARENT_RECT)
                .release(
                    paren = paren,
                    child = child,
                    predictState = state.predictState(offset = offset, zoom = zoom,)
                )
        }
    }

    override fun onParenLayoutCoordinatesUpdate(layoutCoordinates: LayoutCoordinates) {
        paren = layoutCoordinates.boundsInRoot().roundToIntRect()
    }

    override fun onChildLayoutCoordinatesUpdate(layoutCoordinates: LayoutCoordinates) {
        child = layoutCoordinates.boundsInRoot().roundToIntRect()
    }

    private fun TransformState.predictState(
        offset: IntOffset,
        zoom: Float,
    ): TransformState = copy(
        scale = scale * zoom,
        offset = child.center + this.offset + offset,
    )
}

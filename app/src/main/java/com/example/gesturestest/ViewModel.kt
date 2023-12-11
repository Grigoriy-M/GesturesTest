package com.example.gesturestest

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import com.example.gesturestest.util.toLog
import kotlinx.coroutines.flow.StateFlow
import java.math.RoundingMode

interface ViewModel {
    val transformState: StateFlow<TransformState>
    fun onTransformUpdate(offset: IntOffset, zoom: Float)
    fun onTransformEnd()
    fun onChildUpdate(layoutCoordinates: LayoutCoordinates)
    fun onParentUpdate(layoutCoordinates: LayoutCoordinates)
}

class ViewModelImpl : ViewModel {

    private var parent: IntRect = IntRect.Zero
    private var child: IntRect = IntRect.Zero

    private val transformFilterDelegate: TransformFilterDelegate = TransformFilterDelegateImpl()

    override val transformState: StateFlow<TransformState> = transformFilterDelegate.transformState

    override fun onTransformUpdate(offset: IntOffset, zoom: Float) {
        transformFilterDelegate.preprocessingFilter(
            offset = offset,
            zoom = zoom,
            parent = parent,
            child = child,
        )
    }

    override fun onParentUpdate(layoutCoordinates: LayoutCoordinates) {
        parent = layoutCoordinates.boundsInRoot().roundToIntRect()
    }

    override fun onChildUpdate(layoutCoordinates: LayoutCoordinates) {
        child = layoutCoordinates.boundsInRoot().roundToIntRect()
    }

    override fun onTransformEnd() {
        transformFilterDelegate.postprocessingFilter(
            parent = parent,
            child = child,
        )
    }

    internal fun Float.roundTo() = toBigDecimal().setScale(1, RoundingMode.DOWN).toFloat()

//    private fun updateBorderlessMode() {
//        isBorderlessModeEnable = parentRect.factorToOverlaps(childRect) <= BORDERLESS_MODE_FACTOR
//    }
}

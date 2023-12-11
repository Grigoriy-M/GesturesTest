package com.example.gesturestest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.example.gesturestest.ui.theme.GesturesTestTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val transformFilterDelegate: TransformFilterDelegate by lazy {
        TransformFilterDelegateImpl()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GesturesTestTheme {
                Screen(transformFilterDelegate)
            }
        }
    }
}

@Composable
private fun Screen(transformFilterDelegate: TransformFilterDelegate) {
    val state by transformFilterDelegate.transformState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
            .customTransformGestures(
                onGesture = transformFilterDelegate::preprocessingFilter,
                onProgressEnd = transformFilterDelegate::postprocessingFilter,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopCenter),
            text = state.scale.toString(),
            color = Color.Green,
        )
        Box(
            modifier = Modifier
                .background(Color.Green)
                .aspectRatio(16f / 9f)
                .fillMaxSize()
                .onPlaced(transformFilterDelegate::onParentUpdate),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(16f / 7.5f)
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = state.scale,
                        scaleY = state.scale,
                        translationX = state.offset.x.toFloat(),
                        translationY = state.offset.y.toFloat(),
                    )
                    .background(Color.White.copy(alpha = 0.9f))
                    .onPlaced(transformFilterDelegate::onChildUpdate),
            )
        }
    }
}

fun Modifier.customTransformGestures(
    onGesture: (offset: IntOffset, zoom: Float) -> Unit,
    onProgressStart: (() -> Unit)? = null,
    onProgressEnd: (() -> Unit)? = null,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var zoom = 1f
        var offset = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        onProgressStart?.invoke()
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
        onProgressEnd?.invoke()
    }
}

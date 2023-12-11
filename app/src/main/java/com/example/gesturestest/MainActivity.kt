package com.example.gesturestest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import com.example.gesturestest.ui.customTransformGestures
import com.example.gesturestest.ui.theme.GesturesTestTheme
import com.example.gesturestest.util.toLog

class MainActivity : ComponentActivity() {
    private val viewModel: ViewModel by lazy { ViewModelImpl() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GesturesTestTheme {
                Screen(viewModel)
            }
        }
    }
}

@Composable
private fun Screen(viewModel: ViewModel) {
    val state by viewModel.transformState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
            .customTransformGestures(
                onGesture = viewModel::onTransformUpdate,
                onProgressEnd = viewModel::onTransformEnd,
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
                .onPlaced(viewModel::onParentUpdate),
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
                    .onPlaced(viewModel::onChildUpdate),
            )
        }
    }
}

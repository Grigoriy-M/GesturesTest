package com.example.gesturestest

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset

@Stable
data class TransformState(
    val scale: Float = 1f,
    val offset: IntOffset = IntOffset.Zero,
)

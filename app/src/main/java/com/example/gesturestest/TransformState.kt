package com.example.gesturestest

import androidx.compose.ui.unit.IntOffset

data class TransformState(
    val scale: Float = 1f,
    val offset: IntOffset = IntOffset.Zero,
)

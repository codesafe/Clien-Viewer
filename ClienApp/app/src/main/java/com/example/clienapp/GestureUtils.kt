package com.example.clienapp

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

enum class DragAxis { Horizontal, Vertical, None }

@Composable
fun Modifier.smartSwipeBack(
    onBack: () -> Unit,
    onDrag: (Float) -> Unit
): Modifier {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 100.dp.toPx() }
    val touchSlop = with(density) { 16.dp.toPx() }

    return pointerInput(Unit) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    var dragAxis = DragAxis.None
                    var totalDrag = Offset.Zero

                    while (true) {
                        val event = awaitPointerEvent()
                        val dragChange = event.changes.first()

                        if (!dragChange.pressed) {
                            if (dragAxis == DragAxis.Horizontal) {
                                onDrag(0f)
                            }
                            break
                        }

                        totalDrag += dragChange.positionChange()

                        if (dragAxis == DragAxis.None) {
                            if (abs(totalDrag.x) > touchSlop || abs(totalDrag.y) > touchSlop) {
                                if (abs(totalDrag.x) > abs(totalDrag.y)) {
                                    if (totalDrag.x > 0) { // Left to right swipe
                                        dragAxis = DragAxis.Horizontal
                                    } else {
                                        // Right to left swipe, we ignore this.
                                        break
                                    }
                                } else {
                                    // Vertical scroll
                                    dragAxis = DragAxis.Vertical
                                    break // Let other modifiers handle it
                                }
                            }
                        }

                        if (dragAxis == DragAxis.Horizontal) {
                            dragChange.consume()
                            onDrag(totalDrag.x)

                            if (totalDrag.x > swipeThreshold) {
                                onBack()
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.example.clienapp

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope

import kotlin.math.abs

// 제스처 방향
enum class DragAxis { Horizontal, Vertical, None }

@Composable
fun Modifier.smartSwipeBack(onBack: () -> Unit): Modifier {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 100.dp.toPx() } // 스와이프 임계값 증가

    return pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragAxis = DragAxis.None
                var totalHorizontalDrag = 0f

                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.pressed.not() }) break

                    val dragChange = event.changes.first()
                    val dragAmount = dragChange.positionChange()

                    if (dragAxis == DragAxis.None) {
                        if (abs(dragAmount.x) > viewConfiguration.touchSlop || abs(dragAmount.y) > viewConfiguration.touchSlop) {
                            // 대각선 허용: 수직 이동이 수평 이동의 1.5배를 넘지 않으면 수평으로 처리
                            dragAxis = if (abs(dragAmount.y) < abs(dragAmount.x) * 1.5f) DragAxis.Horizontal else DragAxis.Vertical
                        }
                    }

                    if (dragAxis == DragAxis.Horizontal) {
                        totalHorizontalDrag += dragAmount.x
                        if (totalHorizontalDrag > 0) { // 왼쪽에서 오른쪽으로 스와이프
                            dragChange.consume()
                            if (totalHorizontalDrag > swipeThreshold) {
                                onBack()
                                break
                            }
                        }
                    } else if (dragAxis == DragAxis.Vertical) {
                        // 수직 스크롤이 시작되면 제스처 처리를 중단하고 스크롤 컨테이너에 전달
                        break
                    }
                }
            }
        }
    }
}

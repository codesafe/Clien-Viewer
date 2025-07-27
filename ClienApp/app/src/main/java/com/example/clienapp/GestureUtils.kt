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
import kotlinx.coroutines.launch
import kotlin.math.abs

// 제스처 방향
enum class DragAxis { Horizontal, Vertical, None }

@Composable
fun Modifier.smartSwipeBack(onBack: () -> Unit): Modifier {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 100.dp.toPx() } // 스와이프를 인식할 최소 거리

    return pointerInput(Unit) {
        coroutineScope {
            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragAxis = DragAxis.None
                    var totalDragDistance = 0f

                    val slop = viewConfiguration.touchSlop
                    var isDragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed.not() }) {
                            break // 사용자가 손을 뗌
                        }

                        val dragChange = event.changes.first()
                        val dragAmount = dragChange.positionChange()

                        if (!isDragging) {
                            // 터치 슬롭을 넘어섰는지 확인
                            if (abs(dragAmount.x) > slop || abs(dragAmount.y) > slop) {
                                isDragging = true
                                // 드래그 방향 결정
                                dragAxis = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    DragAxis.Horizontal
                                } else {
                                    DragAxis.Vertical
                                }
                            }
                        }

                        if (isDragging && dragAxis == DragAxis.Horizontal) {
                            // 수평 드래그일 때만 처리
                            totalDragDistance += dragAmount.x
                            dragChange.consume() // 다른 컴포넌트가 이벤트를 사용하지 못하도록 막음

                            if (totalDragDistance > swipeThreshold) {
                                launch { onBack() }
                                break
                            }
                        } else if (isDragging && dragAxis == DragAxis.Vertical) {
                            // 수직 드래그가 시작되면, 우리는 더 이상 이벤트를 처리하지 않음
                            // 루프를 중단하여 스크롤 컨테이너가 이벤트를 가져가도록 함
                            break
                        }
                    }
                }
            }
        }
    }
}

package com.example.clienapp

import android.util.Log


import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.swipeBackGesture(
    onSwipeBack: () -> Unit
): Modifier = this.pointerInput(Unit) {
    var startX = 0f
    var startY = 0f
    var hasTriggered = false
    
    Log.d("SwipeGesture", "swipeBackGesture modifier attached")
    
    detectDragGestures(
        onDragStart = { offset ->
            startX = offset.x
            startY = offset.y
            hasTriggered = false
            Log.d("SwipeGesture", "onDragStart - startX: $startX, startY: $startY")
        },
        onDragEnd = {
            Log.d("SwipeGesture", "onDragEnd called, hasTriggered: $hasTriggered")
            hasTriggered = false
        },
        onDrag = { change, dragAmount ->
            val deltaX = change.position.x - startX
            val deltaY = change.position.y - startY
            
//            Log.d("SwipeGesture", "onDrag - position: (${change.position.x}, ${change.position.y}), " +
//                    "delta: ($deltaX, $deltaY), dragAmount: $dragAmount, hasTriggered: $hasTriggered")
            
            // 오른쪽으로 100픽셀 이상 이동하고,
            // 수직 이동이 수평 이동의 1.5배를 넘지 않을 때 (대각선 허용)
            if (!hasTriggered &&
                deltaX > 100f &&
                abs(deltaY) < abs(deltaX) * 1.5f) {
                Log.d("SwipeGesture", "Swipe back triggered! deltaX: $deltaX, startX: $startX")
                hasTriggered = true
                onSwipeBack()
                change.consume()
            }
        }
    )
}
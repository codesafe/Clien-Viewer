package com.example.clienapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("기본 색상") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("사용자 정의") }
            )
        }
        
        when (selectedTab) {
            0 -> PresetColorPicker(selectedColor, onColorSelected)
            1 -> CustomColorPicker(selectedColor, onColorSelected)
        }
    }
}

@Composable
fun PresetColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val presetColors = generateRGBPresetColors()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(16),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(presetColors) { color ->
            RectangleColorItem(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

private fun generateRGBPresetColors(): List<Color> {
    val colors = mutableListOf<Color>()
    
    // RGB 색상환을 기반으로 128가지 색상 생성
    // 8x16 그리드로 구성
    
    // 첫 번째 그룹: 순수 RGB 및 CMY 조합 (32개)
    val primarySteps = listOf(0, 85, 170, 255)
    for (r in primarySteps) {
        for (g in primarySteps) {
            for (b in primarySteps) {
                if (colors.size < 32) {
                    colors.add(Color(r, g, b))
                }
            }
        }
    }
    
    // 두 번째 그룹: HSV 기반 색상환 (48개)
    for (i in 0 until 48) {
        val hue = (i * 7.5f) % 360f // 360도를 48등분
        val saturation = when {
            i < 24 -> 1.0f // 순수 색상
            else -> 0.7f // 약간 흐린 색상
        }
        val value = when {
            i < 24 -> 1.0f
            else -> 0.8f
        }
        colors.add(Color.hsv(hue, saturation, value))
    }
    
    // 세 번째 그룹: 그레이스케일 및 중간 톤 (48개)
    // 그레이스케일 16개
    for (i in 0 until 16) {
        val gray = (i * 255 / 15)
        colors.add(Color(gray, gray, gray))
    }
    
    // 중간 톤 색상들 32개
    val midTones = listOf(
        // 따뜻한 톤
        Color(139, 69, 19), Color(160, 82, 45), Color(210, 180, 140), Color(222, 184, 135),
        Color(245, 245, 220), Color(255, 228, 196), Color(255, 218, 185), Color(255, 160, 122),
        
        // 차가운 톤
        Color(25, 25, 112), Color(65, 105, 225), Color(70, 130, 180), Color(100, 149, 237),
        Color(135, 206, 235), Color(135, 206, 250), Color(173, 216, 230), Color(176, 196, 222),
        
        // 자연 톤
        Color(34, 139, 34), Color(107, 142, 35), Color(124, 252, 0), Color(127, 255, 0),
        Color(173, 255, 47), Color(154, 205, 50), Color(85, 107, 47), Color(128, 128, 0),
        
        // 보라/핑크 톤
        Color(128, 0, 128), Color(147, 112, 219), Color(138, 43, 226), Color(148, 0, 211),
        Color(186, 85, 211), Color(221, 160, 221), Color(238, 130, 238), Color(255, 20, 147)
    )
    colors.addAll(midTones)
    
    return colors.take(128)
}

@Composable
fun CustomColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var red by remember { mutableStateOf(0) }
    var green by remember { mutableStateOf(0) }
    var blue by remember { mutableStateOf(0) }
    
    LaunchedEffect(selectedColor) {
        red = (selectedColor.red * 255).toInt()
        green = (selectedColor.green * 255).toInt()
        blue = (selectedColor.blue * 255).toInt()
    }
    
    LaunchedEffect(red, green, blue) {
        val color = Color(red, green, blue)
        onColorSelected(color)
    }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RGB 색상 정사각형 선택기
        RGBColorSquare(
            red = red,
            green = green,
            blue = blue,
            onColorChange = { newRed, newGreen, newBlue ->
                red = newRed
                green = newGreen
                blue = newBlue
            }
        )
        
        // 색상 미리보기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Color(red, green, blue),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
        )
        
        // Red 슬라이더
        Text("Red: $red", fontWeight = FontWeight.Medium)
        Slider(
            value = red.toFloat(),
            onValueChange = { newRed ->
                red = newRed.toInt()
            },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = Color(red, 0, 0),
                activeTrackColor = Color(255, 0, 0),
                inactiveTrackColor = Color(100, 0, 0)
            )
        )
        
        // Green 슬라이더
        Text("Green: $green", fontWeight = FontWeight.Medium)
        Slider(
            value = green.toFloat(),
            onValueChange = { newGreen ->
                green = newGreen.toInt()
            },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0, green, 0),
                activeTrackColor = Color(0, 255, 0),
                inactiveTrackColor = Color(0, 100, 0)
            )
        )
        
        // Blue 슬라이더
        Text("Blue: $blue", fontWeight = FontWeight.Medium)
        Slider(
            value = blue.toFloat(),
            onValueChange = { newBlue ->
                blue = newBlue.toInt()
            },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0, 0, blue),
                activeTrackColor = Color(0, 0, 255),
                inactiveTrackColor = Color(0, 0, 100)
            )
        )
        
        // RGB 값 표시
        Text(
            text = "RGB: $red, $green, $blue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // HEX 값 표시
        Text(
            text = "HEX: #${String.format("%02X%02X%02X", red, green, blue)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RGBColorSquare(
    red: Int,
    green: Int,
    blue: Int,
    onColorChange: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = offset.x.coerceIn(0f, size.width.toFloat())
                    val y = offset.y.coerceIn(0f, size.height.toFloat())
                    
                    val newRed = (x / size.width * 255).toInt().coerceIn(0, 255)
                    val newGreen = (255 - (y / size.height * 255)).toInt().coerceIn(0, 255)
                    
                    onColorChange(newRed, newGreen, blue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    val sensitivity = 2f
                    val newRed = (red + dragAmount.x * sensitivity).toInt().coerceIn(0, 255)
                    val newGreen = (green - dragAmount.y * sensitivity).toInt().coerceIn(0, 255)
                    
                    onColorChange(newRed, newGreen, blue)
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // RGB 색상 정사각형 그리기 (Red-Green 평면)
        for (x in 0 until canvasWidth.toInt() step 4) {
            for (y in 0 until canvasHeight.toInt() step 4) {
                val r = (x / canvasWidth * 255).toInt().coerceIn(0, 255)
                val g = (255 - (y / canvasHeight * 255)).toInt().coerceIn(0, 255)
                val b = blue
                
                drawRect(
                    color = Color(r, g, b),
                    topLeft = Offset(x.toFloat(), y.toFloat()),
                    size = androidx.compose.ui.geometry.Size(4f, 4f)
                )
            }
        }
        
        // 현재 선택된 위치 표시
        val selectedX = (red / 255f * canvasWidth).coerceIn(0f, canvasWidth)
        val selectedY = ((255 - green) / 255f * canvasHeight).coerceIn(0f, canvasHeight)
        
        // 선택 표시 (외곽 원)
        drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(selectedX, selectedY),
            style = Stroke(width = 3f)
        )
        
        // 선택 표시 (내부 원)
        drawCircle(
            color = Color.Black,
            radius = 8f,
            center = Offset(selectedX, selectedY),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newHue = (offset.x / size.width.toFloat()) * 360f
                    onHueChange(newHue.coerceIn(0f, 360f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newHue = (offset.x / size.width.toFloat()) * 360f
                        onHueChange(newHue.coerceIn(0f, 360f))
                    }
                ) { _, dragAmount ->
                    val currentX = (hue / 360f) * size.width.toFloat()
                    val newX = (currentX + dragAmount.x).coerceIn(0f, size.width.toFloat())
                    val newHue = (newX / size.width.toFloat()) * 360f
                    onHueChange(newHue.coerceIn(0f, 360f))
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // 무지개 그라디언트 그리기
        val gradient = Brush.horizontalGradient(
            colors = listOf(
                Color.hsv(0f, 1f, 1f),   // 빨강
                Color.hsv(60f, 1f, 1f),  // 노랑
                Color.hsv(120f, 1f, 1f), // 초록
                Color.hsv(180f, 1f, 1f), // 청록
                Color.hsv(240f, 1f, 1f), // 파랑
                Color.hsv(300f, 1f, 1f), // 자홍
                Color.hsv(360f, 1f, 1f)  // 빨강
            )
        )
        
        drawRect(
            brush = gradient,
            size = size
        )
        
        // 현재 선택된 색조 위치 표시
        val thumbX = (hue / 360f) * canvasWidth
        
        // 외곽 원 (하얀색 테두리)
        drawCircle(
            color = Color.White,
            radius = canvasHeight / 2 + 4.dp.toPx(),
            center = Offset(thumbX, canvasHeight / 2),
            style = Stroke(width = 3.dp.toPx())
        )
        
        // 내부 원 (현재 색상)
        drawCircle(
            color = Color.hsv(hue, 1f, 1f),
            radius = canvasHeight / 2,
            center = Offset(thumbX, canvasHeight / 2)
        )
    }
}

@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "선택됨",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RectangleColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 20.dp, height = 20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(3.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "선택됨",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun ColorSettingSection(
    title: String,
    description: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showColorPicker = true }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(selectedColor)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
            )
        }
        
        if (showColorPicker) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    ColorPicker(
                        selectedColor = selectedColor,
                        onColorSelected = onColorSelected,
                        modifier = Modifier.height(300.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showColorPicker = false }) {
                            Text("완료")
                        }
                    }
                }
            }
        }
    }
}
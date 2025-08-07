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
    val presetColors = listOf(
        // 기본 Material Colors - 첫 번째 줄 (8개)
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03DAC5), Color(0xFF009688),
        
        // 기본 Material Colors - 두 번째 줄 (8개)
        Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B),
        Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548),
        
        // 그레이 스케일 - 세 번째 줄 (8개)
        Color(0xFF000000), Color(0xFF212121), Color(0xFF424242), Color(0xFF616161),
        Color(0xFF757575), Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFFFFFFFF),
        
        // 파스텔 색상 - 네 번째 줄 (8개)
        Color(0xFFFFCDD2), Color(0xFFF8BBD9), Color(0xFFE1BEE7), Color(0xFFD1C4E9),
        Color(0xFFC5CAE9), Color(0xFFBBDEFB), Color(0xFFB2EBF2), Color(0xFFB2DFDB),
        
        // 연한 색상 - 다섯 번째 줄 (8개)
        Color(0xFFC8E6C9), Color(0xFFDCEDC8), Color(0xFFF0F4C3), Color(0xFFFFF9C4),
        Color(0xFFFFECB3), Color(0xFFFFE0B2), Color(0xFFFFCCBC), Color(0xFFD7CCC8),
        
        // 진한 색상 - 여섯 번째 줄 (8개)
        Color(0xFFB71C1C), Color(0xFF880E4F), Color(0xFF4A148C), Color(0xFF311B92),
        Color(0xFF1A237E), Color(0xFF0D47A1), Color(0xFF01579B), Color(0xFF006064),
        
        // 자연 색상 - 일곱 번째 줄 (8개)
        Color(0xFF1B5E20), Color(0xFF33691E), Color(0xFF827717), Color(0xFFF57F17),
        Color(0xFFFF6F00), Color(0xFFE65100), Color(0xFFBF360C), Color(0xFF3E2723),
        
        // 특별 색상 - 여덟 번째 줄 (8개)
        Color(0xFF263238), Color(0xFF37474F), Color(0xFF455A64), Color(0xFF546E7A),
        Color(0xFF78909C), Color(0xFF90A4AE), Color(0xFFB0BEC5), Color(0xFFCFD8DC)
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presetColors) { color ->
            ColorItem(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun CustomColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    
    LaunchedEffect(selectedColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    
    LaunchedEffect(hue, saturation, value) {
        val color = Color.hsv(hue, saturation, value)
        onColorSelected(color)
    }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 색상 미리보기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Color.hsv(hue, saturation, value),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
        )
        
        // 색조(Hue) 슬라이더
        Text("색조", fontWeight = FontWeight.Medium)
        HueSlider(
            hue = hue,
            onHueChange = { newHue ->
                hue = newHue
            }
        )
        
        // 채도(Saturation) 슬라이더  
        Text("채도", fontWeight = FontWeight.Medium)
        Slider(
            value = saturation,
            onValueChange = { newSaturation ->
                saturation = newSaturation
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.hsv(hue, saturation, value),
                activeTrackColor = Color.hsv(hue, 0.8f, value),
                inactiveTrackColor = Color.hsv(hue, 0.2f, value)
            )
        )
        
        // 명도(Value) 슬라이더
        Text("명도", fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = { newValue ->
                value = newValue
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.hsv(hue, saturation, value),
                activeTrackColor = Color.hsv(hue, saturation, 0.8f),
                inactiveTrackColor = Color.hsv(hue, saturation, 0.2f)
            )
        )
        
        // RGB 값 표시
        Text(
            text = "RGB: ${(selectedColor.red * 255).toInt()}, ${(selectedColor.green * 255).toInt()}, ${(selectedColor.blue * 255).toInt()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
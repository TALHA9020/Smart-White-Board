package com.smart.whiteboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class DrawingLine(val path: Path, val color: Color, val strokeWidth: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WhiteboardApp() }
    }
}

@Composable
fun WhiteboardApp() {
    val lines = remember { mutableStateListOf<DrawingLine>() }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }
    var isExpanded by remember { mutableStateOf(false) }
    var isPencilMode by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val drawColor = if (isPencilMode) currentColor else Color.White
                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                    lines.add(DrawingLine(newPath, drawColor, strokeWidth))
                },
                onDrag = { change, _ ->
                    change.consume()
                    lines.lastOrNull()?.path?.lineTo(change.position.x, change.position.y)
                    val last = lines.removeAt(lines.size - 1)
                    lines.add(last)
                }
            )
        }) {
            lines.forEach { line ->
                drawPath(path = line.path, color = line.color,
                    style = Stroke(width = line.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(25.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(25.dp))
                    .padding(8.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isExpanded) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingSmallButton(
                            icon = if (isPencilMode) Icons.Default.Edit else Icons.Default.Brush,
                            bgColor = Color(0xFF4CAF50),
                            onClick = { isPencilMode = !isPencilMode }
                        )
                        FloatingSmallButton(
                            icon = Icons.Default.Delete,
                            bgColor = Color(0xFFF44336),
                            onClick = { lines.clear() }
                        )
                        FloatingSmallButton(
                            icon = Icons.Default.ArrowDropDownCircle,
                            bgColor = Color(0xFF2196F3),
                            onClick = { isExpanded = true }
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size", color = Color.White, modifier = Modifier.padding(end = 8.dp))
                        Slider(value = strokeWidth, onValueChange = { strokeWidth = it }, valueRange = 5f..80f, modifier = Modifier.width(120.dp))
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(Icons.Default.ArrowCircleUp, "Close", tint = Color.Cyan)
                        }
                    }
                    Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow)
                        colors.forEach { color ->
                            Box(modifier = Modifier.size(32.dp).background(color, CircleShape)
                                .border(if (currentColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { currentColor = color; isPencilMode = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingSmallButton(icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(45.dp)
            .background(bgColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

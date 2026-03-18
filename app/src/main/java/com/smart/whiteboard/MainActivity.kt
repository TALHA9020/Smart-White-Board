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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class DrawingLine(val path: Path, val color: Color, val strokeWidth: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // پٹی کو ہٹانے کے لیے ونڈو کو فل سکرین پر سیٹ کرنا
        setContent { WhiteboardApp() }
    }
}

@Composable
fun WhiteboardApp() {
    val lines = remember { mutableStateListOf<DrawingLine>() }
    val undoneLines = remember { mutableStateListOf<DrawingLine>() }
    
    var currentColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }
    var isExpanded by remember { mutableStateOf(true) }
    var isPencilMode by remember { mutableStateOf(true) }
    var confirmRequired by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Control Panel Position
    var panelOffset by remember { mutableStateOf(Offset(100f, 100f)) }
    var scale by remember { mutableStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 2f)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Drawing Canvas - اب یہ پوری سکرین پر پھیلا ہوا ہے
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val drawColor = if (isPencilMode) currentColor else Color.White
                    val finalWidth = if (isPencilMode) strokeWidth else strokeWidth * 4f
                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                    lines.add(DrawingLine(newPath, drawColor, finalWidth))
                    undoneLines.clear()
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

        // Draggable Control Panel
        Box(
            modifier = Modifier
                .offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) }
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panelOffset += dragAmount
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF212121), RoundedCornerShape(28.dp))
                    .border(1.dp, Color.Gray.copy(0.4f), RoundedCornerShape(28.dp))
                    .padding(12.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isExpanded) {
                    // Line 1: Tools & Colors
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { isPencilMode = true }, 
                            modifier = Modifier.background(if(isPencilMode) Color.Green else Color.Transparent, CircleShape)) {
                            Icon(Icons.Default.Edit, "Pencil", tint = if(isPencilMode) Color.Black else Color.White)
                        }
                        IconButton(onClick = { isPencilMode = false },
                            modifier = Modifier.background(if(!isPencilMode) Color.White else Color.Transparent, CircleShape)) {
                            Icon(Icons.Default.AutoFixNormal, "Eraser", tint = Color.Black)
                        }
                        
                        val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow)
                        colors.forEach { color ->
                            Box(modifier = Modifier.size(26.dp).background(color, CircleShape)
                                .border(if (currentColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { currentColor = color; isPencilMode = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.DarkGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Line 2: Clear, Slider, Undo/Redo, Fold
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = { if (confirmRequired) showDeleteDialog = true else lines.clear() }) {
                            Icon(Icons.Default.Delete, "Clear", tint = Color.Red)
                        }
                        Checkbox(checked = confirmRequired, onCheckedChange = { confirmRequired = it },
                            colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray))
                        
                        Slider(value = strokeWidth, onValueChange = { strokeWidth = it }, valueRange = 5f..100f, modifier = Modifier.width(80.dp))
                        
                        // Size Preview
                        Box(modifier = Modifier.size((strokeWidth/5).coerceIn(4f, 15f).dp).background(if(isPencilMode) currentColor else Color.LightGray, CircleShape))

                        IconButton(onClick = { if (lines.isNotEmpty()) undoneLines.add(lines.removeAt(lines.size - 1)) }) {
                            Icon(Icons.Default.Undo, "Undo", tint = Color.White)
                        }
                        IconButton(onClick = { if (undoneLines.isNotEmpty()) lines.add(undoneLines.removeAt(undoneLines.size - 1)) }) {
                            Icon(Icons.Default.Redo, "Redo", tint = Color.White)
                        }
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(Icons.Default.ExpandLess, "Fold", tint = Color.Cyan)
                        }
                    }
                } else {
                    // Folded Mode: 3 Capsule Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        // Green Button: Toggles Pencil/Eraser
                        Box(Modifier.size(45.dp, 20.dp).background(if(isPencilMode) Color.Green else Color.LightGray, CircleShape)
                            .clickable { isPencilMode = !isPencilMode }) // یہاں ٹوگل لاجک ٹھیک کر دی
                        
                        // Red Button: Clear All
                        Box(Modifier.size(45.dp, 20.dp).background(Color.Red, CircleShape)
                            .clickable { if (confirmRequired) showDeleteDialog = true else lines.clear() })
                        
                        // Blue Button: Expand Panel
                        Box(Modifier.size(45.dp, 20.dp).background(Color.Blue, CircleShape)
                            .clickable { isExpanded = true })
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("تصدیق") },
                text = { Text("کیا آپ سارا بورڈ صاف کرنا چاہتے ہیں؟") },
                confirmButton = { TextButton(onClick = { lines.clear(); showDeleteDialog = false }) { Text("ہاں", color = Color.Red) } },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("نہیں") } }
            )
        }
    }
}

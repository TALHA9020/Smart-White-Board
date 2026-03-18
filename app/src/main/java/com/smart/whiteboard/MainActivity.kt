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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
    val undoneLines = remember { mutableStateListOf<DrawingLine>() }
    
    var currentColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }
    var isExpanded by remember { mutableStateOf(true) }
    var isPencilMode by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var confirmRequired by remember { mutableStateOf(true) }

    // Control Panel State
    var panelOffset by remember { mutableStateOf(Offset(20f, 20f)) }
    var scale by remember { mutableStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 2f)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Drawing Area
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val drawColor = if (isPencilMode) currentColor else Color.White
                    val finalWidth = if (isPencilMode) strokeWidth else strokeWidth * 2f
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

        // Draggable and Transformable Control Panel
        Box(
            modifier = Modifier
                .offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panelOffset += dragAmount
                    }
                }
                .transformable(state = transformState)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            ControlPanelUI(
                isExpanded = isExpanded,
                isPencilMode = isPencilMode,
                currentColor = currentColor,
                strokeWidth = strokeWidth,
                confirmRequired = confirmRequired,
                onToggleMode = { isPencilMode = it },
                onColorSelect = { currentColor = it; isPencilMode = true },
                onWidthChange = { strokeWidth = it },
                onUndo = { if (lines.isNotEmpty()) undoneLines.add(lines.removeAt(lines.size - 1)) },
                onRedo = { if (undoneLines.isNotEmpty()) lines.add(undoneLines.removeAt(undoneLines.size - 1)) },
                onClear = { 
                    if (confirmRequired) showDeleteConfirm = true else lines.clear() 
                },
                onFold = { isExpanded = !it },
                onConfirmToggle = { confirmRequired = it }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("تصدیق") },
                text = { Text("کیا آپ سارا بورڈ صاف کرنا چاہتے ہیں؟") },
                confirmButton = {
                    Button(onClick = { lines.clear(); showDeleteConfirm = false }) { Text("ہاں") }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirm = false }) { Text("نہیں") }
                }
            )
        }
    }
}

@Composable
fun ControlPanelUI(
    isExpanded: Boolean,
    isPencilMode: Boolean,
    currentColor: Color,
    strokeWidth: Float,
    confirmRequired: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onColorSelect: (Color) -> Unit,
    onWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onFold: (Boolean) -> Unit,
    onConfirmToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(20.dp))
            .padding(12.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isExpanded) {
            // Line 1: Pencil, Eraser, Color Menu
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CapsuleButton(
                    icon = Icons.Default.Edit, 
                    label = "Pencil", 
                    isActive = isPencilMode, 
                    activeColor = Color.Green,
                    onClick = { onToggleMode(true) }
                )
                CapsuleButton(
                    icon = Icons.Default.AutoFixNormal, 
                    label = "Eraser", 
                    isActive = !isPencilMode, 
                    activeColor = Color.White,
                    onClick = { onToggleMode(false) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow)
                    colors.forEach { color ->
                        Box(modifier = Modifier.size(28.dp).background(color, CircleShape)
                            .border(if (currentColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { onColorSelect(color) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Line 2: Clear, Checkbox, Size, Undo/Redo, Fold
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Delete, "Clear", tint = Color.Red)
                }
                Checkbox(checked = confirmRequired, onCheckedChange = onConfirmToggle)
                
                Slider(value = strokeWidth, onValueChange = onWidthChange, valueRange = 5f..80f, modifier = Modifier.width(80.dp))
                
                // Preview
                Box(modifier = Modifier.size((strokeWidth/2).dp).background(if(isPencilMode) currentColor else Color.LightGray, CircleShape))

                IconButton(onClick = onUndo) { Icon(Icons.Default.Undo, "Undo", tint = Color.White) }
                IconButton(onClick = onRedo) { Icon(Icons.Default.Redo, "Redo", tint = Color.White) }
                
                IconButton(onClick = { onFold(true) }) {
                    Icon(Icons.Default.KeyboardArrowUp, "Fold", tint = Color.Cyan)
                }
            }
        } else {
            // Folded Mode: 3 Capsule Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp, 20.dp).background(if(isPencilMode) Color.Green else Color.Gray, CircleShape).clickable { onToggleMode(true) })
                Box(Modifier.size(40.dp, 20.dp).background(Color.Red, CircleShape).clickable { onClear() })
                Box(Modifier.size(40.dp, 20.dp).background(Color.Blue, CircleShape).clickable { onFold(false) })
            }
        }
    }
}

@Composable
fun CapsuleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = if (isActive) activeColor else Color.DarkGray,
        contentColor = if (isActive) Color.Black else Color.White
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}

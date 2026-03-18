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
import androidx.compose.material.icons.rounded.*
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

    // Control Panel Floating State
    var panelOffset by remember { mutableStateOf(Offset(50f, 50f)) }
    var scale by remember { mutableStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.7f, 1.5f)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // --- DRAWING CANVAS ---
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val drawColor = if (isPencilMode) currentColor else Color.White
                    // اریزر کا سائز پنسل سے بڑا
                    val finalWidth = if (isPencilMode) strokeWidth else strokeWidth * 3f
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
                drawPath(
                    path = line.path, 
                    color = line.color,
                    style = Stroke(width = line.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // --- DRAGGABLE CONTROL PANEL ---
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
            ControlPanelContent(
                isExpanded = isExpanded,
                isPencilMode = isPencilMode,
                currentColor = currentColor,
                strokeWidth = strokeWidth,
                confirmRequired = confirmRequired,
                onModeChange = { isPencilMode = it },
                onColorChange = { currentColor = it; isPencilMode = true },
                onWidthChange = { strokeWidth = it },
                onUndo = { if (lines.isNotEmpty()) undoneLines.add(lines.removeAt(lines.size - 1)) },
                onRedo = { if (undoneLines.isNotEmpty()) lines.add(undoneLines.removeAt(undoneLines.size - 1)) },
                onClear = { if (confirmRequired) showDeleteDialog = true else lines.clear() },
                onFoldToggle = { isExpanded = !isExpanded },
                onConfirmToggle = { confirmRequired = it }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("تصدیق") },
                text = { Text("کیا آپ پورا بورڈ صاف کرنا چاہتے ہیں؟") },
                confirmButton = {
                    TextButton(onClick = { lines.clear(); showDeleteDialog = false }) { Text("جی ہاں", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("کینسل") }
                }
            )
        }
    }
}

@Composable
fun ControlPanelContent(
    isExpanded: Boolean,
    isPencilMode: Boolean,
    currentColor: Color,
    strokeWidth: Float,
    confirmRequired: Boolean,
    onModeChange: (Boolean) -> Unit,
    onColorChange: (Color) -> Unit,
    onWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onFoldToggle: () -> Unit,
    onConfirmToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .background(Color(0xFF252525), RoundedCornerShape(24.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(12.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isExpanded) {
            // Line 1: Pencil, Eraser, Colors
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton(Icons.Default.Edit, "Pencil", isPencilMode, Color.Green) { onModeChange(true) }
                ModeButton(Icons.Default.AutoFixNormal, "Eraser", !isPencilMode, Color.White) { onModeChange(false) }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow)
                colors.forEach { color ->
                    Box(modifier = Modifier
                        .size(26.dp)
                        .background(color, CircleShape)
                        .border(if (currentColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onColorChange(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Line 2: Clear, Size, Undo/Redo, Fold
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onClear) { Icon(Icons.Default.Delete, "Clear", tint = Color.Red) }
                
                Checkbox(checked = confirmRequired, onCheckedChange = onConfirmToggle, 
                    colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray))
                
                Slider(value = strokeWidth, onValueChange = onWidthChange, valueRange = 4f..100f, modifier = Modifier.width(70.dp))
                
                // Preview Circle
                Box(modifier = Modifier.size((strokeWidth/4).coerceIn(4f, 20f).dp).background(if(isPencilMode) currentColor else Color.LightGray, CircleShape))

                IconButton(onClick = onUndo) { Icon(Icons.Default.Undo, "Undo", tint = Color.White) }
                IconButton(onClick = onRedo) { Icon(Icons.Default.Redo, "Redo", tint = Color.White) }
                
                IconButton(onClick = onFoldToggle) { Icon(Icons.Default.ExpandLess, "Fold", tint = Color.Cyan) }
            }
        } else {
            // Folded Mode: 3 Capsule Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(45.dp, 18.dp).background(if(isPencilMode) Color.Green else Color.Gray, CircleShape).clickable { onModeChange(true) })
                Box(Modifier.size(45.dp, 18.dp).background(Color.Red, CircleShape).clickable { onClear() })
                Box(Modifier.size(45.dp, 18.dp).background(Color.Blue, CircleShape).clickable { onFoldToggle() })
            }
        }
    }
}

@Composable
fun ModeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeColor else Color.Transparent,
        contentColor = if (selected) Color.Black else Color.White,
        modifier = Modifier.height(36.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

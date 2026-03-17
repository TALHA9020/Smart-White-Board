package com.smart.whiteboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// لکیر کا ڈیٹا
data class DrawingLine(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhiteboardApp()
        }
    }
}

@Composable
fun WhiteboardApp() {
    val lines = remember { mutableStateListOf<DrawingLine>() }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // اوپر والا کنٹرول بار
        SmallTopAppBar(
            title = { Text("Smart Whiteboard") },
            actions = {
                IconButton(onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) }) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { lines.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color.Red)
                }
            }
        )

        // ڈرائنگ ایریا (سفید بورڈ)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newPath = Path().apply { moveTo(offset.x, offset.y) }
                            lines.add(DrawingLine(newPath, currentColor, strokeWidth))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            lines.lastOrNull()?.path?.lineTo(change.position.x, change.position.y)
                            // ری ڈرا کرنے کے لیے لسٹ کو ری فریش کرنا
                            val last = lines.removeAt(lines.size - 1)
                            lines.add(last)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                lines.forEach { line ->
                    drawPath(
                        path = line.path,
                        color = line.color,
                        style = Stroke(
                            width = line.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // نیچے رنگوں اور سائز کا پینل
        BottomToolBar(
            onColorSelect = { currentColor = it },
            onSizeChange = { strokeWidth = it },
            currentWidth = strokeWidth
        )
    }
}

@Composable
fun BottomToolBar(onColorSelect: (Color) -> Unit, onSizeChange: (Float) -> Unit, currentWidth: Float) {
    Surface(tonalElevation = 8.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Magenta)
                colors.forEach { color ->
                    Button(
                        onClick = { onColorSelect(color) },
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        modifier = Modifier.size(40.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        content = {}
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Brush Size")
            Slider(
                value = currentWidth,
                onValueChange = onSizeChange,
                valueRange = 2f..50f
            )
        }
    }
}

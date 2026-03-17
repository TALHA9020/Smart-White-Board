package com.smart.whiteboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*

// لکیر کے ڈیٹا کا ڈھانچہ
data class Line(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

class FloatingControlService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var controlView: ComposeView? = null
    private var canvasView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // سٹیٹ جو پینل اور بورڈ دونوں شیئر کریں گے
    private val lines = mutableStateListOf<Line>()
    private var currentColor = mutableStateOf(Color.White)
    private var currentSize = mutableFloatStateOf(10f)
    private var isPencilMode = mutableStateOf(true)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startMyForeground()
    }

    private fun startMyForeground() {
        val channelId = "whiteboard_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Smart Board", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Smart White Board")
            .setContentText("بورڈ اور پینل دونوں فعال ہیں")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showCanvas() // لکھنے والا بورڈ
        showControlPanel() // بٹنوں والا پینل
        return START_STICKY
    }

    // فل سکرین شفاف بورڈ (Canvas)
    private fun showCanvas() {
        val canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        canvasView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingControlService)
            setViewTreeViewModelStoreOwner(this@FloatingControlService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControlService)
            setContent {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                    lines.add(Line(newPath, currentColor.value, currentSize.floatValue))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    lines.lastOrNull()?.path?.lineTo(change.position.x, change.position.y)
                                    // زبردستی ری ڈرا کروانے کے لیے
                                    val last = lines.removeAt(lines.size - 1)
                                    lines.add(last)
                                }
                            )
                        }
                    ) {
                        lines.forEach { line ->
                            drawPath(
                                path = line.path,
                                color = line.color,
                                style = Stroke(width = line.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        }
        windowManager.addView(canvasView, canvasParams)
    }

    // بٹنوں والا کنٹرول پینل
    private fun showControlPanel() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100; y = 200
        }

        controlView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingControlService)
            setViewTreeViewModelStoreOwner(this@FloatingControlService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControlService)
            setContent { ControlPanelUI(params, this) }
        }

        // پینل کو ڈریگ (موو) کرنے کی لاجک
        controlView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0
            private var initialTouchX = 0f; private var initialTouchY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(controlView, params)
                        return true
                    }
                }
                return false
            }
        })
        windowManager.addView(controlView, params)
    }

    @Composable
    fun ControlPanelUI(params: WindowManager.LayoutParams, view: View) {
        var isExpanded by remember { mutableStateOf(true) }
        var scale by remember { mutableFloatStateOf(1f) }
        var askBeforeDelete by remember { mutableStateOf(true) }

        Box(modifier = Modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(0.5f, 2.0f) }
            }
            .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(8.dp)
        ) {
            if (isExpanded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(IntrinsicSize.Min)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ToolButton(Icons.Default.Edit, "Pencil", isPencilMode.value) { isPencilMode.value = true; currentColor.value = Color.White }
                        IconButton(onClick = { currentColor.value = Color.Transparent; isPencilMode.value = false }) {
                            Icon(Icons.Default.AutoFixNormal, null, tint = if(!isPencilMode.value) Color.Red else Color.Gray)
                        }
                        Slider(value = currentSize.floatValue, onValueChange = { currentSize.floatValue = it }, valueRange = 5f..80f, modifier = Modifier.width(100.dp))
                    }
                    Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White)
                        colors.forEach { color ->
                            Box(modifier = Modifier.padding(4.dp).size(25.dp).background(color, CircleShape)
                                .border(if (currentColor.value == color) 2.dp else 0.dp, Color.Cyan, CircleShape)
                                .clickable { currentColor.value = color; isPencilMode.value = true }
                            )
                        }
                        IconButton(onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) }) { Icon(Icons.Default.Undo, null, tint = Color.LightGray) }
                        IconButton(onClick = { if(!askBeforeDelete || lines.isEmpty()) lines.clear() }) { Icon(Icons.Default.DeleteForever, null, tint = Color.Red) }
                        Checkbox(checked = askBeforeDelete, onCheckedChange = { askBeforeDelete = it })
                        IconButton(onClick = { isExpanded = false }) { Icon(Icons.Default.UnfoldLess, null, tint = Color.Cyan) }
                    }
                }
            } else {
                Row {
                    Box(modifier = Modifier.size(35.dp).background(Color.Green, CircleShape).clickable { isExpanded = true })
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(35.dp).background(Color.Red, CircleShape).clickable { lines.clear() })
                }
            }
        }
    }

    @Composable
    fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, isSelected: Boolean, onClick: () -> Unit) {
        IconButton(onClick = onClick, modifier = Modifier.background(if(isSelected) Color.DarkGray else Color.Transparent, CircleShape)) {
            Icon(icon, contentDescription = desc, tint = if(isSelected) Color.Green else Color.White)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        canvasView?.let { windowManager.removeView(it) }
        controlView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }
}

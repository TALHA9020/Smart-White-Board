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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*

class FloatingClockService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

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
            .setContentText("کنٹرول پینل فعال ہے")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFloatingPanel()
        return START_STICKY
    }

    private fun showFloatingPanel() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingClockService)
            setViewTreeViewModelStoreOwner(this@FloatingClockService)
            setViewTreeSavedStateRegistryOwner(this@FloatingClockService)

            setContent {
                ControlPanelUI()
            }
        }

        // ڈریگ کرنے کی لاجک
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0; private var initialY: Int = 0
            private var initialTouchX: Float = 0f; private var initialTouchY: Float = 0f
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
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
        windowManager.addView(floatingView, params)
    }

    @Composable
    fun ControlPanelUI() {
        var isExpanded by remember { mutableStateOf(true) }
        var scale by remember { mutableFloatStateOf(1f) }
        var pencilSelected by remember { mutableStateOf(true) }
        var highlighterSelected by remember { mutableStateOf(false) }
        var askBeforeDelete by remember { mutableStateOf(true) }
        var selectedColor by remember { mutableStateOf(Color.White) }
        var toolSize by remember { mutableFloatStateOf(5f) }

        // پینل کا مین ڈیزائن
        Box(
            modifier = Modifier
                .scale(scale)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(0.5f, 2.5f) }
                }
                .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            if (isExpanded) {
                // Expanded Mode: Slim and Long
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    // Row 1: Tools & Size
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                        ToolButton(Icons.Default.Edit, "Pencil", pencilSelected) {
                            pencilSelected = !pencilSelected; if(pencilSelected) highlighterSelected = false
                        }
                        ToolButton(Icons.Default.Brush, "Highlighter", highlighterSelected) {
                            highlighterSelected = !highlighterSelected; if(highlighterSelected) pencilSelected = false
                        }
                        // Eraser Indicator (Auto)
                        val eraserActive = !pencilSelected && !highlighterSelected
                        Icon(Icons.Default.AutoFixNormal, null, tint = if(eraserActive) Color.Red else Color.DarkGray, modifier = Modifier.size(28.dp))
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Size Slider & Preview
                        Slider(value = toolSize, onValueChange = { toolSize = it }, valueRange = 1f..50f, modifier = Modifier.width(80.dp))
                        Box(modifier = Modifier.size((toolSize/2).coerceIn(4f, 20f).dp).background(selectedColor, CircleShape))
                    }

                    Divider(color = Color.Gray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Row 2: Colors & Actions
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White)
                        colors.forEach { color ->
                            Box(modifier = Modifier
                                .padding(horizontal = 4.dp).size(24.dp)
                                .background(color, CircleShape)
                                .border(if (selectedColor == color) 2.dp else 0.dp, Color.Cyan, CircleShape)
                                .clickable { selectedColor = color }
                            )
                        }
                        
                        IconButton(onClick = { /* Undo */ }) { Icon(Icons.Default.Undo, null, tint = Color.LightGray) }
                        IconButton(onClick = { /* Redo */ }) { Icon(Icons.Default.Redo, null, tint = Color.LightGray) }
                        
                        // Delete All
                        IconButton(onClick = { if(!askBeforeDelete) { /* Clear All */ } }) {
                            Icon(Icons.Default.DeleteForever, null, tint = Color.Red)
                        }
                        Checkbox(checked = askBeforeDelete, onCheckedChange = { askBeforeDelete = it })
                        
                        // Fold Button
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(Icons.Default.UnfoldLess, null, tint = Color.Cyan)
                        }
                    }
                }
            } else {
                // Folded Mode: 3 Buttons on Top
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Green: Pencil/Eraser Toggle
                    Box(modifier = Modifier.size(30.dp).background(Color.Green, CircleShape).clickable { pencilSelected = !pencilSelected })
                    Spacer(modifier = Modifier.width(10.dp))
                    // Red: Clear All
                    Box(modifier = Modifier.size(30.dp).background(Color.Red, CircleShape).clickable { /* Clear All */ })
                    Spacer(modifier = Modifier.width(10.dp))
                    // Blue: Unfold
                    Box(modifier = Modifier.size(30.dp).background(Color.Blue, CircleShape).clickable { isExpanded = true })
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
        floatingView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        super.onDestroy()
    }
}

package com.example.jpc1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        context = this,
                        onStartClick = { checkAndStartService() }
                    )
                }
            }
        }
    }

    private fun checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 1001)
            } else {
                startFloatingService()
            }
        } else {
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingClockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingService()
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context, onStartClick: () -> Unit) {
    val prefs = context.getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)
    
    var sizeScale by remember { mutableFloatStateOf(prefs.getFloat("size_scale", 1f)) }
    var opacity by remember { mutableFloatStateOf(prefs.getFloat("opacity", 1f)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Golden Floating Clock", 
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Size: ${(sizeScale * 100).toInt()}%")
        Slider(
            value = sizeScale,
            onValueChange = { 
                sizeScale = it
                prefs.edit().putFloat("size_scale", it).apply()
            },
            valueRange = 0.5f..2.0f
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Opacity: ${(opacity * 100).toInt()}%")
        Slider(
            value = opacity,
            onValueChange = { 
                opacity = it
                prefs.edit().putFloat("opacity", it).apply()
            },
            valueRange = 0.2f..1.0f
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = "Launch / Update Clock")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tip: Drag capsule to the very top of the screen to remove it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

package com.vibedev.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.vibedev.app.ui.GameScreen
import com.vibedev.app.ui.LifeController
import com.vibedev.app.ui.Settings
import com.vibedev.app.ui.theme.VibeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeAppTheme {
                val controller = remember { LifeController(Settings(applicationContext)) }
                GameScreen(controller)
                CrashReportDialog()
            }
        }
    }
}

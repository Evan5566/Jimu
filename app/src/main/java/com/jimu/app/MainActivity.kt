package com.jimu.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.jimu.app.navigation.AppNavHost
import com.jimu.app.ui.theme.JimuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        setContent {
            JimuTheme {
                SideEffect {
                    window.statusBarColor = Color.TRANSPARENT
                    window.navigationBarColor = Color.TRANSPARENT

                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
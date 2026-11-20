package com.example.go2hmi

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.go2hmi.ui.RoboGuardTheme
import com.example.go2hmi.ui.RobotApp
import com.example.go2hmi.viewmodel.RobotViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RobotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            RoboGuardTheme { RobotApp(viewModel) }
        }
    }
}

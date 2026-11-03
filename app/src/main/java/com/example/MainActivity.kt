package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.DroidStudioTheme
import com.example.ui.theme.VsCodeBg
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DroidStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VsCodeBg
                ) {
                    val viewModel: MainViewModel = viewModel()
                    WorkspaceScreen(viewModel = viewModel)
                }
            }
        }
    }
}

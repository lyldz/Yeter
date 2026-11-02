package com.example.kumbara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KumbaraApp()
        }
    }
}

@Composable
fun KumbaraApp(viewModel: KumbaraViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kumbara Tracker") }) }
    ) { paddingValues ->
        KumbaraScreen(state = state, onNoSpend = { viewModel.incrementStreak() })
    }
}

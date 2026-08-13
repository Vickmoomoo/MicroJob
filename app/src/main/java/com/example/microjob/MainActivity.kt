package com.example.microjob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.microjob.ui.navigation.MicroJobApp
import com.example.microjob.ui.theme.MicroJobTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MicroJobTheme {
                MicroJobApp()
            }
        }
    }
}

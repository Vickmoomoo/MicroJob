package com.example.microjob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.microjob.ui.navigation.MicroJobApp
import com.example.microjob.ui.theme.MicroJobTheme
import com.example.microjob.data.AppPreferences
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = AppPreferences(applicationContext)
            val theme by preferences.theme.collectAsState()
            MicroJobTheme(
                darkTheme = when (theme) {
                    AppPreferences.THEME_DARK -> true
                    AppPreferences.THEME_LIGHT -> false
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            ) {
                MicroJobApp()
            }
        }
    }
}

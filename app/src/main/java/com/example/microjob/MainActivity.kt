package com.example.microjob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.data.AppPreferences
import com.example.microjob.ui.navigation.MicroJobApp
import com.example.microjob.ui.theme.MicroJobTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = AppPreferences.getInstance(applicationContext)
            val theme by preferences.theme.collectAsStateWithLifecycle()
            MicroJobTheme(
                darkTheme = when (theme) {
                    AppPreferences.THEME_DARK -> true
                    AppPreferences.THEME_LIGHT -> false
                    else -> isSystemInDarkTheme()
                }
            ) {
                MicroJobApp()
            }
        }
    }
}

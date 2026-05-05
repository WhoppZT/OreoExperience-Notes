package com.oreoexperience.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.nav.AppNav
import com.oreoexperience.notes.ui.theme.OreoExperienceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OreoApp).container
        setContent {
            OreoExperienceTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppNav()
                }
            }
        }
    }
}

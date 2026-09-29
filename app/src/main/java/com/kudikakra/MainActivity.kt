package com.kudikakra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kudikakra.ui.KudiKakraApp
import com.kudikakra.ui.theme.KudiKakraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KudiKakraTheme {
                KudiKakraApp()
            }
        }
    }
}

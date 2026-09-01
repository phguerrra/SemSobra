package com.example.semsobra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.semsobra.ui.SemSobraApp
import com.example.semsobra.ui.SemSobraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SemSobraTheme {
                SemSobraApp()
            }
        }
    }
}

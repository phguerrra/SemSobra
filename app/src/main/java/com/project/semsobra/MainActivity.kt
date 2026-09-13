package com.project.semsobra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.project.semsobra.ui.SemSobraApp
import com.project.semsobra.ui.SemSobraTheme

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

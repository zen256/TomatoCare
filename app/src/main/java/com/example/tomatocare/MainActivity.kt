package com.example.tomatocare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tomatocare.navigation.AppNavigation
import com.example.tomatocare.ui.theme.TomatoCareTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TomatoCareTheme {
                AppNavigation()
            }
        }
    }
}
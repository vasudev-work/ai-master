package com.vasudev.ai.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vasudev.ai.master.ui.AppRoot
import com.vasudev.ai.master.ui.theme.MyAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAiTheme {
                AppRoot()
            }
        }
    }
}

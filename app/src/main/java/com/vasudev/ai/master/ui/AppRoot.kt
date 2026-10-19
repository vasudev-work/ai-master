package com.vasudev.ai.master.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vasudev.ai.master.ui.about.AboutScreen
import com.vasudev.ai.master.ui.chat.ChatScreen
import com.vasudev.ai.master.ui.settings.SettingsScreen

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                onOpenSettings = { navController.navigate("settings") },
                onOpenAbout = { navController.navigate("about") }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

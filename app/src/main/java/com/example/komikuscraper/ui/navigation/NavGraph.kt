package com.example.komikuscraper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.komikuscraper.ui.home.HomeScreen
import com.example.komikuscraper.ui.features.komiku.KomikuHomeScreen
import com.example.komikuscraper.ui.features.komiku.KomikuDetailScreen
import com.example.komikuscraper.ui.features.komiku.ReaderScreen
import com.example.komikuscraper.ui.features.winbu.WinbuHomeScreen
import com.example.komikuscraper.ui.features.winbu.WinbuDetailScreen
import com.example.komikuscraper.ui.features.winbu.PlayerScreen

@Composable
fun KomikuNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        // Komiku
        composable(Screen.KomikuHome.route) {
            KomikuHomeScreen(navController)
        }
        composable(
            route = Screen.MangaDetail.route,
            arguments = listOf(navArgument("endpoint") { type = NavType.StringType })
        ) { backStackEntry ->
             val endpoint = backStackEntry.arguments?.getString("endpoint") ?: ""
             KomikuDetailScreen(navController, endpoint)
        }
        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("endpoint") { type = NavType.StringType })
        ) { backStackEntry ->
            val endpoint = backStackEntry.arguments?.getString("endpoint") ?: ""
            ReaderScreen(endpoint)
        }
        
        // Winbu
        composable(Screen.WinbuHome.route) {
            WinbuHomeScreen(navController)
        }
        composable(
            route = Screen.AnimeDetail.route,
            arguments = listOf(navArgument("endpoint") { type = NavType.StringType })
        ) { backStackEntry ->
             val endpoint = backStackEntry.arguments?.getString("endpoint") ?: ""
             WinbuDetailScreen(navController, endpoint)
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("endpoint") { type = NavType.StringType })
        ) { backStackEntry ->
             val endpoint = backStackEntry.arguments?.getString("endpoint") ?: ""
             PlayerScreen(endpoint)
        }
    }
}

package com.example.komikuscraper.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    
    // Komiku
    object KomikuHome : Screen("komiku_home")
    object MangaDetail : Screen("manga_detail/{endpoint}") {
        fun createRoute(endpoint: String) = "manga_detail/${java.net.URLEncoder.encode(endpoint, "UTF-8")}"
    }
    object Reader : Screen("reader/{endpoint}") {
        fun createRoute(endpoint: String) = "reader/${java.net.URLEncoder.encode(endpoint, "UTF-8")}"
    }
    
    // Winbu
    object WinbuHome : Screen("winbu_home")
    object AnimeDetail : Screen("anime_detail/{endpoint}") {
        fun createRoute(endpoint: String) = "anime_detail/${java.net.URLEncoder.encode(endpoint, "UTF-8")}"
    }
    object Player : Screen("player/{endpoint}") {
        fun createRoute(endpoint: String) = "player/${java.net.URLEncoder.encode(endpoint, "UTF-8")}"
    }
}

package com.example.komikuscraper.ui.features.winbu

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.komikuscraper.data.model.winbu.Anime
import com.example.komikuscraper.ui.features.komiku.TopAppBar // Reuse
import com.example.komikuscraper.ui.navigation.Screen

@Composable
fun WinbuHomeScreen(
    navController: NavController,
    viewModel: WinbuViewModel = hiltViewModel()
) {
    val homeData by viewModel.homeState.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHome()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Winbu - Anime & Movie") }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
             if (loading && homeData == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } 
            
            homeData?.let { data ->
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item { SectionTitle("Latest Anime") }
                    item { HorizontalAnimeList(data.latestAnime, navController) }
                    
                    item { SectionTitle("Latest Movies") }
                    item { HorizontalAnimeList(data.latestMovies, navController) }
                    
                    item { SectionTitle("Top Series") }
                    item { HorizontalAnimeList(data.topSeries, navController) }
                    
                    item { SectionTitle("Top Movies") }
                    item { HorizontalAnimeList(data.topMovies, navController) }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun HorizontalAnimeList(list: List<Anime>, navController: NavController) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(list) { anime ->
            AnimeItem(anime) {
                navController.navigate(Screen.AnimeDetail.createRoute(anime.endpoint))
            }
        }
    }
}

@Composable
fun AnimeItem(anime: Anime, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = anime.thumb,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Text(
                text = anime.title,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
fun WinbuDetailScreen(
    navController: NavController,
    endpoint: String,
    viewModel: WinbuViewModel = hiltViewModel()
) {
    val detail by viewModel.detailState.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(endpoint) {
        viewModel.loadDetail(endpoint)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(detail?.title ?: "Loading...") }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
             if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } 
            
            detail?.let { anime ->
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                         Row(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = anime.thumb,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(150.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = anime.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(text = "Score: ${anime.score}")
                                anime.metadata.forEach { (k, v) ->
                                    Text(text = "$k: $v", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = anime.synopsis, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Episodes", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    items(anime.episodes) { ep ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    navController.navigate(Screen.Player.createRoute(ep.endpoint))
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = ep.title,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(
    endpoint: String,
    viewModel: WinbuViewModel = hiltViewModel()
) {
    val episode by viewModel.episodeState.collectAsState()
    val streamUrl by viewModel.streamUrl.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(endpoint) {
        viewModel.loadEpisode(endpoint)
    }

    // Auto-resolve first option if available and not yet resolved
    LaunchedEffect(episode) {
        if (streamUrl == null && episode != null && episode!!.streamOptions.isNotEmpty()) {
            viewModel.resolveStream(episode!!.streamOptions[0])
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (streamUrl != null) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(streamUrl!!)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        } else {
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator()
                else Text("Select a server to play")
            }
        }
        
        episode?.let { ep ->
            Text(
                text = ep.title, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp, 
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Servers",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ep.streamOptions) { opt ->
                    Button(onClick = { viewModel.resolveStream(opt) }) {
                        Text("${opt.name} (${opt.quality})")
                    }
                }
            }
            
            if (ep.downloadLinks.isNotEmpty()) {
                 Text(
                    text = "Downloads",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ep.downloadLinks) { link ->
                        Button(
                            onClick = { viewModel.downloadVideo(link.url, ep.title, link.quality) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                             Text(link.server + " (" + link.quality + ")")
                        }
                    }
                }
            }
        }
    }
}

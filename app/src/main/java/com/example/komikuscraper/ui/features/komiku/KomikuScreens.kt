package com.example.komikuscraper.ui.features.komiku

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.komikuscraper.data.model.komiku.Manga
import com.example.komikuscraper.ui.navigation.Screen

@Composable
fun KomikuHomeScreen(
    navController: NavController,
    viewModel: KomikuViewModel = hiltViewModel()
) {
    val homeManga by viewModel.homeState.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Komiku - Terbaru") })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (loading && homeManga.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(homeManga) { manga ->
                        MangaItem(manga) {
                            navController.navigate(Screen.MangaDetail.createRoute(manga.endpoint))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaItem(manga: Manga, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = manga.thumb,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Text(
                text = manga.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
fun KomikuDetailScreen(
    navController: NavController,
    endpoint: String,
    viewModel: KomikuViewModel = hiltViewModel()
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
            
            detail?.let { manga ->
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = manga.thumb,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(150.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = manga.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(text = "Status: ${manga.status}")
                                Text(text = "Author: ${manga.authors.joinToString(", ")}")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = manga.synopsis, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Chapters", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(manga.chapters) { chapter ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    navController.navigate(Screen.Reader.createRoute(chapter.endpoint))
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = chapter.dateUploaded,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(
    endpoint: String,
    viewModel: KomikuViewModel = hiltViewModel()
) {
    val images by viewModel.chapterState.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(endpoint) {
        viewModel.loadChapter(endpoint)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(images) { img ->
                    AsyncImage(
                        model = img.url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
            // FAB for Download
            FloatingActionButton(
                onClick = {
                    images.forEachIndexed { index, img ->
                        // Extract chapter/title if possible, or pass as args. 
                        // For simplicity, using generic names or we need to pass Manga/Chapter info to ReaderScreen.
                        // Assuming user knows what they are downloading.
                         viewModel.downloadChapterImage(img.url, "Manga", "Chapter", index)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("DL")
            }
        }
    }
}

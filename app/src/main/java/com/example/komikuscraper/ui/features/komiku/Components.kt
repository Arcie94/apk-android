package com.example.komikuscraper.ui.features.komiku

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
) {
    CenterAlignedTopAppBar(title = title)
}

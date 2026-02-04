package com.example.komikuscraper.data.model.komiku

data class Manga(
    val title: String,
    val endpoint: String,
    val thumb: String
)

data class MangaDetail(
    val title: String,
    val thumb: String,
    val synopsis: String,
    val authors: List<String>,
    val status: String,
    val genres: List<String>,
    val chapters: List<ChapterLink>
)

data class ChapterLink(
    val title: String,
    val endpoint: String,
    val dateUploaded: String
)

data class ChapterImage(
    val url: String
)

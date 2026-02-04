package com.example.komikuscraper.data.model.winbu

data class Anime(
    val title: String,
    val endpoint: String,
    val thumb: String,
    val rating: String = "",
    val status: String = ""
)

data class AnimeDetail(
    val title: String,
    val thumb: String,
    val synopsis: String,
    val score: String,
    val genres: List<String>,
    val episodes: List<Episode>,
    val metadata: Map<String, String>
)

data class Episode(
    val title: String,
    val endpoint: String
)

data class EpisodePageData(
    val title: String,
    val streamOptions: List<StreamOption>,
    val nextEpisodeEndpoint: String = "",
    val prevEpisodeEndpoint: String = "",
    val downloadLinks: List<DownloadLink> = emptyList()
)

data class StreamOption(
    val name: String,
    val server: String,
    val quality: String,
    val postId: String,
    val nume: String,
    val type: String
)

data class DownloadLink(
    val server: String,
    val url: String,
    val quality: String
)

data class HomeData(
    val latestAnime: List<Anime> = emptyList(),
    val latestMovies: List<Anime> = emptyList(),
    val topSeries: List<Anime> = emptyList(),
    val topMovies: List<Anime> = emptyList(),
    val internationalSeries: List<Anime> = emptyList(),
    val genres: List<Genre> = emptyList()
)

data class Genre(
    val name: String,
    val endpoint: String
)

package com.example.komikuscraper.data.remote.scraper

import com.example.komikuscraper.data.model.winbu.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WinbuScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val baseUrl = "https://winbu.net"

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Referer", "$baseUrl/")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Failed to fetch $url: ${response.code}")
        response.body?.string() ?: throw IOException("Empty body for $url")
    }

    suspend fun getHomeData(): HomeData {
        val html = fetchHtml(baseUrl)
        val doc = Jsoup.parse(html)
        val data = HomeData(
            latestAnime = mutableListOf(),
            latestMovies = mutableListOf(),
            topSeries = mutableListOf(),
            topMovies = mutableListOf(),
            internationalSeries = mutableListOf(),
            genres = mutableListOf()
        )

        doc.select(".movies-list-wrap").forEach { section ->
            val title = section.select(".list-title h2").text().lowercase()
            val animeList = mutableListOf<Anime>()
            
            section.select(".ml-item").forEach { item ->
               animeList.add(extractAnimeFromItem(item))
            }

            // Using when-like logic or if-else
            when {
                title.contains("series") && title.contains("top 10") -> (data.topSeries as MutableList).addAll(animeList)
                title.contains("film") && title.contains("top 10") -> (data.topMovies as MutableList).addAll(animeList)
                title.contains("anime donghua terbaru") || title.contains("anime terbaru") -> (data.latestAnime as MutableList).addAll(animeList)
                title.contains("film terbaru") -> (data.latestMovies as MutableList).addAll(animeList)
                title.contains("jepang korea china barat") -> (data.internationalSeries as MutableList).addAll(animeList)
            }
        }
        
        // Genres
        doc.select("#List-Anime .list-group-item a").forEach {
            val name = it.text().trim()
            val endpoint = it.attr("href")
            if (name.isNotEmpty() && endpoint.isNotEmpty() && !name.lowercase().contains("daftar anime")) {
                (data.genres as MutableList).add(Genre(name, endpoint))
            }
        }

        return data
    }
    
    suspend fun searchAnime(query: String): List<Anime> {
        val searchUrl = "$baseUrl/?s=$query" // Winbu uses + for spaces, okhttp handles URL encoding properly? better safe.
        // Actually Jsoup connection handles it, but here we build url string manually.
        // But with OkHttp request builder... spaces might need encoding.
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/?s=$encodedQuery"
        
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)
        val results = mutableListOf<Anime>()
        
        doc.select(".a-item").forEach { item ->
            results.add(extractAnimeFromItem(item))
        }
        return results
    }

    suspend fun getAnimeDetail(endpoint: String): AnimeDetail {
        val url = if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)
        
        val container = doc.select(".movies-list.movies-list-full .t-item")
        
        var title = container.select(".mli-info .judul").text().trim()
        if (title.isEmpty()) title = doc.select("h1.titless").text().trim()
        
        val thumb = container.select(".ml-mask .mli-thumb-box img").attr("src")
        val synopsis = container.select(".ml-mask .mli-desc").text().trim()
        val score = container.select(".ml-mask .mli-mvi span[itemprop='ratingValue']").text().trim()
        
        val genres = mutableListOf<String>()
        container.select(".ml-mask .mli-mvi a[itemprop='genre']").forEach {
            genres.add(it.text().trim())
        }
        
        val episodes = mutableListOf<Episode>()
        doc.select(".tvseason .les-content a").forEach {
            episodes.add(Episode(it.text().trim(), it.attr("href")))
        }
        
        // Fallback for movies
        if (episodes.isEmpty()) {
            episodes.add(Episode("Full Movie / Watch", url))
        }
        
        val metadata = mutableMapOf<String, String>()
        container.select(".mli-mvi").forEach { 
            val text = it.text()
            when {
                text.contains("Status :") -> metadata["Status"] = text.replace("Status :", "").trim()
                text.contains("Duration :") -> metadata["Duration"] = text.replace("Duration :", "").trim()
                text.contains("Negara :") -> metadata["Country"] = text.replace("Negara :", "").trim()
                text.contains("Released :") -> metadata["Released"] = text.replace("Released :", "").trim()
            }
        }
        
        return AnimeDetail(title, thumb, synopsis, score, genres, episodes, metadata)
    }

    suspend fun getEpisode(endpoint: String): EpisodePageData {
        val url = if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)
        
        var title = doc.select("h1.titless").text().trim()
        if (title.isEmpty()) title = doc.select("title").text().trim()
        
        val streamOptions = mutableListOf<StreamOption>()
        doc.select(".east_player_option").forEach { opt ->
            val text = opt.text().trim()
            val postId = opt.attr("data-post")
            val nume = opt.attr("data-nume")
            val type = opt.attr("data-type")
            
            // Simple parsing for name/quality
            var quality = "SD"
            if (text.contains("1080")) quality = "1080p"
            else if (text.contains("720")) quality = "720p"
            else if (text.contains("480")) quality = "480p"
            else if (text.contains("360")) quality = "360p"
            
            val name = text // simplified
            
            if (postId.isNotEmpty() && nume.isNotEmpty()) {
                streamOptions.add(StreamOption(name, name, quality, postId, nume, type))
            }
        }
        
        val nextEp = doc.select(".naveps .nvsc a:contains(Next)").attr("href") ?: ""
        val prevEp = doc.select(".naveps .nvsc a:contains(Prev)").attr("href") ?: ""
        
        return EpisodePageData(title, streamOptions, nextEp, prevEp)
    }

    suspend fun resolveStream(option: StreamOption): String = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("action", "player_ajax")
            .add("post", option.postId)
            .add("nume", option.nume)
            .add("type", option.type)
            .build()
            
        val request = Request.Builder()
            .url("$baseUrl/wp-admin/admin-ajax.php")
            .header("Referer", "$baseUrl/")
            .header("X-Requested-With", "XMLHttpRequest") // Sometimes needed
            .post(formBody)
            .build()
            
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Failed to resolve stream: ${response.code}")
        val body = response.body?.string() ?: ""
        
        val doc = Jsoup.parse(body)
        var src = doc.select("iframe").attr("src")
        if (src.isEmpty()) src = doc.select("iframe").attr("data-src")
        
        if (src.isEmpty()) throw IOException("No iframe source found")
        
        src
    }

    private fun extractAnimeFromItem(element: org.jsoup.nodes.Element): Anime {
        var title = element.select(".mli-info h2").text().trim()
        if (title.isEmpty()) title = element.select(".mli-info .judul").text().trim()
        if (title.isEmpty()) title = element.select("a.ml-mask").attr("title").trim()
        
        var thumb = element.select("img").attr("data-original")
        if (thumb.isEmpty()) thumb = element.select("img").attr("src")
        
        val endpoint = element.select("a").first()?.attr("href") ?: ""
        
        return Anime(title, endpoint, thumb)
    }
}

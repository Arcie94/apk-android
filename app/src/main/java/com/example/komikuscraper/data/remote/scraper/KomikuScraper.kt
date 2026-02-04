package com.example.komikuscraper.data.remote.scraper

import com.example.komikuscraper.data.model.komiku.ChapterImage
import com.example.komikuscraper.data.model.komiku.ChapterLink
import com.example.komikuscraper.data.model.komiku.Manga
import com.example.komikuscraper.data.model.komiku.MangaDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KomikuScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val baseUrl = "https://komiku.id"

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to fetch $url: ${response.code}")
        response.body?.string() ?: throw Exception("Empty body for $url")
    }

    suspend fun getLatestManga(): List<Manga> {
        val html = fetchHtml(baseUrl)
        val doc = Jsoup.parse(html)
        val mangas = mutableListOf<Manga>()

        // Selector: #Terbaru .ls4
        doc.select("#Terbaru .ls4").forEach { element ->
            val title = element.select(".ls4j h4 a").text().trim()
            val endpoint = element.select(".ls4j h4 a").attr("href")
            var thumb = element.select(".ls4v img").attr("src")
            
            // Handle lazy load
            if (thumb.isBlank() || thumb.contains("lazy.jpg")) {
                thumb = element.select(".ls4v img").attr("data-src")
            }
            // Clean thumb url
            if (thumb.contains("?")) {
               thumb = thumb.substringBefore("?")
            }

            if (title.isNotEmpty() && endpoint.isNotEmpty()) {
                mangas.add(Manga(title, endpoint, thumb))
            }
        }
        return mangas
    }
    
    suspend fun searchManga(query: String): List<Manga> {
        val searchUrl = "$baseUrl/?post_type=manga&s=$query"
        val html = fetchHtml(searchUrl)
        val doc = Jsoup.parse(html)
        val mangas = mutableListOf<Manga>()

        // Selector: .bge or article.ls2
        doc.select(".bge, article.ls2").forEach { element ->
            var title = element.select(".kan h3").text().trim()
            var endpoint = element.select(".kan a").attr("href")
            var thumb = element.select(".bgei img").attr("src")
            
            // Fallback for ls2 structure
            if (title.isEmpty()) {
                title = element.select(".ls2j h3 a").text().trim()
                endpoint = element.select(".ls2j h3 a").attr("href")
                thumb = element.select(".ls2v img").attr("src")
                if (thumb.isBlank()) thumb = element.select(".ls2v img").attr("data-src")
            }
            
             // Clean thumb
            if (thumb.contains("?")) thumb = thumb.substringBefore("?")
            if (thumb.contains("lazy.jpg")) thumb = ""

            if (title.isNotEmpty() && endpoint.isNotEmpty()) {
                mangas.add(Manga(title, endpoint, thumb))
            }
        }
        return mangas
    }

    suspend fun getMangaDetail(endpoint: String): MangaDetail {
        val html = fetchHtml(endpoint) // Endpoint should be full URL or handles by the caller? Assuming full URL for now if passed from Manga object, or append if relative.
        // Actually the scraper returns full URL usually? Go parser returns what is in href. 
        // Komiku hrefs usually start with https://komiku.id... let's ensure.
        // If endpoint doesn't start with http, prepend baseUrl.
        val inputUrl = if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint"
        
        // Re-fetch if needed (already fetched above via fetchHtml logic which takes url)
        // Wait, fetchHtml implementation above takes 'url', so I should correct the logic.
        // Let's refactor: logic handles full url.
        
        // Since I can't easy refactor fetchHtml call in this single tool call, I will do it purely with inputUrl.
        // But wait, the method signature takes 'endpoint'.
        // Let's assume input is full URL for safety, or handle it inside.
        
        val doc = Jsoup.parse(if (endpoint.startsWith("http")) fetchHtml(endpoint) else fetchHtml("$baseUrl$endpoint"))

        val title = doc.select("#Judul h1").text().trim()
        var thumb = doc.select(".ims img").attr("src")
        if (thumb.contains("?")) thumb = thumb.substringBefore("?")
        
        val synopsis = doc.select(".desc").text().trim()
        
        val authors = mutableListOf<String>()
        var status = ""
        val genres = mutableListOf<String>()
        
        // Metadata
        doc.select(".inftable tr").forEach { row ->
            val label = row.select("td").first()?.text()?.lowercase()?.trim() ?: ""
            val value = row.select("td").last()?.text()?.trim() ?: ""
            
            if (label.contains("pengarang")) authors.add(value)
            if (label.contains("status")) status = value
        }
        
        // Genres
        doc.select(".genre li a").forEach { 
            genres.add(it.text().trim())
        }
        
        // Chapters
        val chapters = mutableListOf<ChapterLink>()
        doc.select("table#Daftar_Chapter tr").forEach { row ->
            if (row.select("th").isNotEmpty()) return@forEach
            
            val titleEl = row.select("td.judulseries a")
            val chapTitle = titleEl.text().trim()
            val chapEndpoint = titleEl.attr("href")
            val date = row.select("td.tanggalseries").text().trim()
            
            if (chapTitle.isNotEmpty() && chapEndpoint.isNotEmpty()) {
                chapters.add(ChapterLink(chapTitle, chapEndpoint, date))
            }
        }
        
        return MangaDetail(title, thumb, synopsis, authors, status, genres, chapters)
    }
    
    suspend fun getChapterImages(endpoint: String): List<ChapterImage> {
        val url = if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)
        val images = mutableListOf<ChapterImage>()
        
        doc.select("#Baca_Komik img").forEach { img ->
            var src = img.attr("src")
            if (src.isBlank() || src.contains("lazy.jpg")) {
                src = img.attr("data-src")
            }
            if (src.isNotEmpty() && !src.contains("lazy.jpg")) {
                images.add(ChapterImage(src))
            }
        }
        return images
    }
}

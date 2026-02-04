package com.example.komikuscraper.ui.features.komiku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.komikuscraper.data.model.komiku.ChapterImage
import com.example.komikuscraper.data.model.komiku.Manga
import com.example.komikuscraper.data.model.komiku.MangaDetail
import com.example.komikuscraper.data.remote.scraper.KomikuScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KomikuViewModel @Inject constructor(
    private val scraper: KomikuScraper,
    private val downloadRepository: com.example.komikuscraper.data.repository.DownloadRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow<List<Manga>>(emptyList())
    val homeState: StateFlow<List<Manga>> = _homeState.asStateFlow()

    private val _detailState = MutableStateFlow<MangaDetail?>(null)
    val detailState: StateFlow<MangaDetail?> = _detailState.asStateFlow()

    private val _chapterState = MutableStateFlow<List<ChapterImage>>(emptyList())
    val chapterState: StateFlow<List<ChapterImage>> = _chapterState.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    // Add Search State
    private val _searchState = MutableStateFlow<List<Manga>>(emptyList())
    val searchState: StateFlow<List<Manga>> = _searchState.asStateFlow()

    fun loadHome() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _homeState.value = scraper.getLatestManga()
            } catch (e: Exception) {
                // handle error
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun searchManga(query: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _searchState.value = scraper.searchManga(query)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun downloadChapterImage(url: String, title: String, chapter: String, index: Int) {
        val filename = "image_$index.jpg"
        downloadRepository.downloadImage(url, title, chapter, filename)
    }

    fun loadDetail(endpoint: String) {
        viewModelScope.launch {
            _loading.value = true
            _detailState.value = null // Reset
            try {
                _detailState.value = scraper.getMangaDetail(endpoint)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadChapter(endpoint: String) {
        viewModelScope.launch {
            _loading.value = true
            _chapterState.value = emptyList() // Reset
            try {
                _chapterState.value = scraper.getChapterImages(endpoint)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}

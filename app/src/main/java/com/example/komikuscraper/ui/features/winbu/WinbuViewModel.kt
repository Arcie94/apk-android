package com.example.komikuscraper.ui.features.winbu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.komikuscraper.data.model.winbu.AnimeDetail
import com.example.komikuscraper.data.model.winbu.EpisodePageData
import com.example.komikuscraper.data.model.winbu.HomeData
import com.example.komikuscraper.data.model.winbu.StreamOption
import com.example.komikuscraper.data.remote.scraper.WinbuScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WinbuViewModel @Inject constructor(
    private val scraper: WinbuScraper,
    private val downloadRepository: com.example.komikuscraper.data.repository.DownloadRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeData?>(null)
    val homeState: StateFlow<HomeData?> = _homeState.asStateFlow()

    private val _detailState = MutableStateFlow<AnimeDetail?>(null)
    val detailState: StateFlow<AnimeDetail?> = _detailState.asStateFlow()

    private val _episodeState = MutableStateFlow<EpisodePageData?>(null)
    val episodeState: StateFlow<EpisodePageData?> = _episodeState.asStateFlow()
    
    // Stream URL
    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadHome() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _homeState.value = scraper.getHomeData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadDetail(endpoint: String) {
        viewModelScope.launch {
            _loading.value = true
            _detailState.value = null
            try {
                _detailState.value = scraper.getAnimeDetail(endpoint)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadEpisode(endpoint: String) {
        viewModelScope.launch {
            _loading.value = true
            _episodeState.value = null
            _streamUrl.value = null
            try {
                _episodeState.value = scraper.getEpisode(endpoint)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun downloadVideo(url: String, title: String, quality: String) {
        val filename = "${title}_$quality.mp4"
        downloadRepository.downloadVideo(url, title, filename)
    }

    fun resolveStream(option: StreamOption) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _streamUrl.value = scraper.resolveStream(option)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}

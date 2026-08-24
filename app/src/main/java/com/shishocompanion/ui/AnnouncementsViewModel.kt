package com.shishocompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.BuildConfig
import com.shishocompanion.data.Announcement
import com.shishocompanion.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    private val client: HttpClient,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val baseUrl: String = BuildConfig.SHISHO_API_URL
    private val pollIntervalMs: Long = 5 * 60 * 1000L // 5 minutes

    private val refreshTrigger = MutableStateFlow(0L)

    private suspend fun fetchRemoteAnnouncements(): List<Announcement>? {
        return try {
            client.get("$baseUrl/api/announcements").body<List<Announcement>>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val announcements: StateFlow<List<Announcement>> = refreshTrigger
        .flatMapLatest {
            flow {
                while (currentCoroutineContext().isActive) {
                    fetchRemoteAnnouncements()?.let { emit(it) }
                    delay(pollIntervalMs)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hasUnreadAnnouncement: StateFlow<Boolean> = combine(
        announcements,
        userPrefs.lastReadAnnouncementId
    ) { announcementList, lastReadId ->
        val latestAnnouncement = announcementList.lastOrNull()
        if (latestAnnouncement != null) {
            latestAnnouncement.id != lastReadId
        } else {
            false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun fetchAnnouncements() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    fun markAsRead() {
        val latestAnnouncement = announcements.value.lastOrNull()
        if (latestAnnouncement != null) {
            viewModelScope.launch {
                userPrefs.setLastReadAnnouncementId(latestAnnouncement.id)
            }
        }
    }
}

package com.shishocompanion.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.Announcement
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.shishocompanion.BuildConfig

class AnnouncementsViewModel(
    private val client: HttpClient,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _hasUnreadAnnouncement = MutableStateFlow(false)
    val hasUnreadAnnouncement: StateFlow<Boolean> = _hasUnreadAnnouncement.asStateFlow()

    private val baseUrl: String = BuildConfig.SHISHO_API_URL

    init {
        fetchAnnouncements()
    }

    fun fetchAnnouncements() {
        viewModelScope.launch {
            try {
                val response: List<Announcement> = client.get("$baseUrl/api/announcements").body()
                _announcements.value = response
                
                // Only consider the most recent announcement
                val latestAnnouncement = response.lastOrNull()
                if (latestAnnouncement != null) {
                    val lastReadId = sharedPrefs.getString("LAST_READ_ANNOUNCEMENT_ID", "")
                    _hasUnreadAnnouncement.value = latestAnnouncement.id != lastReadId
                } else {
                    _hasUnreadAnnouncement.value = false
                }
            } catch (e: Exception) {
                // Exception handled silently
            }
        }
    }

    fun markAsRead() {
        val latestAnnouncement = _announcements.value.lastOrNull()
        if (latestAnnouncement != null) {
            sharedPrefs.edit().putString("LAST_READ_ANNOUNCEMENT_ID", latestAnnouncement.id).apply()
            _hasUnreadAnnouncement.value = false
        }
    }
}

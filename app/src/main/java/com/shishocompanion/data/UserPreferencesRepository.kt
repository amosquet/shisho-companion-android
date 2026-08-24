package com.shishocompanion.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_PREFERENCE = stringPreferencesKey("THEME_PREFERENCE")
        val TIME_FORMAT_PREFERENCE = stringPreferencesKey("TIME_FORMAT_PREFERENCE")
        val DATE_FORMAT_PREFERENCE = stringPreferencesKey("DATE_FORMAT_PREFERENCE")
        val LANGUAGE_PREFERENCE = stringPreferencesKey("LANGUAGE_PREFERENCE")
        val SWIPE_ACTION = stringPreferencesKey("SWIPE_ACTION")
        val GOOGLE_BOOKS_API_KEY = stringPreferencesKey("GOOGLE_BOOKS_API_KEY")
        val DISCORD_DISPLAY_ID = stringPreferencesKey("DISCORD_DISPLAY_ID")
        val LAST_READ_ANNOUNCEMENT_ID = stringPreferencesKey("LAST_READ_ANNOUNCEMENT_ID")
        val SEND_REMINDERS_TO_DISCORD = booleanPreferencesKey("SEND_REMINDERS_TO_DISCORD")
        val SPLIT_SCREEN_ENABLED = booleanPreferencesKey("SPLIT_SCREEN_ENABLED")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("DYNAMIC_COLOR_ENABLED")
    }

    val themePreference: Flow<String> = dataStore.data.map { it[THEME_PREFERENCE] ?: "SYSTEM" }
    val dynamicColorEnabled: Flow<Boolean> = dataStore.data.map { it[DYNAMIC_COLOR_ENABLED] ?: false }
    val timeFormatPreference: Flow<String> = dataStore.data.map { it[TIME_FORMAT_PREFERENCE] ?: "SYSTEM" }
    val dateFormatPreference: Flow<String> = dataStore.data.map { it[DATE_FORMAT_PREFERENCE] ?: "SYSTEM" }
    val languagePreference: Flow<String> = dataStore.data.map { it[LANGUAGE_PREFERENCE] ?: "SYSTEM" }
    val swipeAction: Flow<String> = dataStore.data.map { it[SWIPE_ACTION] ?: "DELETE" }
    val googleBooksApiKey: Flow<String> = dataStore.data.map { it[GOOGLE_BOOKS_API_KEY] ?: "" }
    val discordDisplayId: Flow<String> = dataStore.data.map { it[DISCORD_DISPLAY_ID] ?: "" }
    val lastReadAnnouncementId: Flow<String> = dataStore.data.map { it[LAST_READ_ANNOUNCEMENT_ID] ?: "" }
    val sendRemindersToDiscord: Flow<Boolean> = dataStore.data.map { it[SEND_REMINDERS_TO_DISCORD] ?: false }
    val splitScreenEnabled: Flow<Boolean> = dataStore.data.map { it[SPLIT_SCREEN_ENABLED] ?: false }

    suspend fun setThemePreference(value: String) {
        dataStore.edit { it[THEME_PREFERENCE] = value }
    }

    suspend fun setTimeFormatPreference(value: String) {
        dataStore.edit { it[TIME_FORMAT_PREFERENCE] = value }
    }

    suspend fun setDateFormatPreference(value: String) {
        dataStore.edit { it[DATE_FORMAT_PREFERENCE] = value }
    }

    suspend fun setLanguagePreference(value: String) {
        dataStore.edit { it[LANGUAGE_PREFERENCE] = value }
    }

    suspend fun setSwipeAction(value: String) {
        dataStore.edit { it[SWIPE_ACTION] = value }
    }

    suspend fun setGoogleBooksApiKey(value: String) {
        dataStore.edit { it[GOOGLE_BOOKS_API_KEY] = value }
    }

    suspend fun setDiscordDisplayId(value: String) {
        dataStore.edit { it[DISCORD_DISPLAY_ID] = value }
    }

    suspend fun setLastReadAnnouncementId(value: String) {
        dataStore.edit { it[LAST_READ_ANNOUNCEMENT_ID] = value }
    }

    suspend fun setSendRemindersToDiscord(value: Boolean) {
        dataStore.edit { it[SEND_REMINDERS_TO_DISCORD] = value }
    }

    suspend fun setSplitScreenEnabled(value: Boolean) {
        dataStore.edit { it[SPLIT_SCREEN_ENABLED] = value }
    }

    suspend fun setDynamicColorEnabled(value: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_ENABLED] = value }
    }

    suspend fun updateFromRemote(preferences: com.shishocompanion.data.Preferences?, discordId: String?) {
        dataStore.edit { prefs ->
            if (preferences != null) {
                if (preferences.companion.theme.isNotBlank()) {
                    prefs[THEME_PREFERENCE] = preferences.companion.theme
                }
                preferences.companion.splitScreen?.let {
                    prefs[SPLIT_SCREEN_ENABLED] = it
                }
                if (preferences.general.timeFormat.isNotBlank()) {
                    prefs[TIME_FORMAT_PREFERENCE] = preferences.general.timeFormat
                }
                if (preferences.general.dateFormat.isNotBlank()) {
                    prefs[DATE_FORMAT_PREFERENCE] = preferences.general.dateFormat
                }
                if (preferences.general.language.isNotBlank()) {
                    prefs[LANGUAGE_PREFERENCE] = preferences.general.language
                }
                prefs[SEND_REMINDERS_TO_DISCORD] = preferences.general.sendRemindersToDiscord
            }
            if (discordId != null) {
                prefs[DISCORD_DISPLAY_ID] = discordId
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}


package com.cozy.herbshop.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val CURRENT_LEVEL = intPreferencesKey("current_level")
        val STAMINA_COUNT = intPreferencesKey("stamina_count")
        val LAST_STAMINA_REGEN_TIME = longPreferencesKey("last_stamina_regen_time")
        val UNLOCKED_HERBS = stringSetPreferencesKey("unlocked_herbs")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val THEME_PACK_UNLOCKED = booleanPreferencesKey("theme_pack_unlocked")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val COMPLETED_LEVELS_COUNT = intPreferencesKey("completed_levels_count")

        const val MAX_STAMINA = 5
        const val STAMINA_REGEN_INTERVAL_MS = 15 * 60 * 1000L
    }

    val currentLevelFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[CURRENT_LEVEL] ?: 1
    }

    val staminaCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STAMINA_COUNT] ?: MAX_STAMINA
    }

    val unlockedHerbsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[UNLOCKED_HERBS] ?: setOf("chamomile", "lavender", "mint")
    }

    val adsRemovedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ADS_REMOVED] ?: false
    }

    val themePackUnlockedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[THEME_PACK_UNLOCKED] ?: false
    }

    val selectedThemeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_THEME] ?: "cozy_default"
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SOUND_ENABLED] ?: true
    }

    val completedLevelsCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COMPLETED_LEVELS_COUNT] ?: 0
    }

    suspend fun updateCurrentLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_LEVEL] = level
        }
    }

    suspend fun incrementCompletedLevels() {
        context.dataStore.edit { prefs ->
            val count = prefs[COMPLETED_LEVELS_COUNT] ?: 0
            prefs[COMPLETED_LEVELS_COUNT] = count + 1
        }
    }

    suspend fun consumeStamina(): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val current = prefs[STAMINA_COUNT] ?: MAX_STAMINA
            if (current > 0) {
                prefs[STAMINA_COUNT] = current - 1
                if (prefs[LAST_STAMINA_REGEN_TIME] == null || prefs[LAST_STAMINA_REGEN_TIME] == 0L) {
                    prefs[LAST_STAMINA_REGEN_TIME] = System.currentTimeMillis()
                }
                success = true
            }
        }
        return success
    }

    suspend fun addStamina(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[STAMINA_COUNT] ?: MAX_STAMINA
            prefs[STAMINA_COUNT] = minOf(MAX_STAMINA, current + amount)
        }
    }

    suspend fun unlockHerb(herbId: String) {
        context.dataStore.edit { prefs ->
            val currentSet = prefs[UNLOCKED_HERBS] ?: setOf("chamomile", "lavender", "mint")
            prefs[UNLOCKED_HERBS] = currentSet + herbId
        }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ADS_REMOVED] = removed
        }
    }

    suspend fun setThemePackUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[THEME_PACK_UNLOCKED] = unlocked
        }
    }

    suspend fun setSelectedTheme(themeId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_THEME] = themeId
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SOUND_ENABLED] = enabled
        }
    }
}

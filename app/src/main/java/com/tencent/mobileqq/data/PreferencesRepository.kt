package com.tencent.mobileqq.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherDataStore by preferencesDataStore(name = "launcher_preferences")

class PreferencesRepository(private val context: Context) {
    private val favoritePackagesKey = stringSetPreferencesKey("favorite_packages")

    val userPreferences: Flow<UserPreferences> = context.launcherDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::toUserPreferences)

    suspend fun toggleFavorite(packageName: String) {
        context.launcherDataStore.edit { preferences ->
            val current = preferences[favoritePackagesKey].orEmpty().toMutableSet()
            if (!current.add(packageName)) current.remove(packageName)
            preferences[favoritePackagesKey] = current
        }
    }

    suspend fun recordSuccessfulOpen(packageName: String, timestamp: Long) {
        context.launcherDataStore.edit { preferences ->
            preferences[recentKey(packageName)] = timestamp
        }
    }

    suspend fun removeStalePackages(validPackages: Set<String>) {
        context.launcherDataStore.edit { preferences ->
            val validFavorites = preferences[favoritePackagesKey].orEmpty().intersect(validPackages)
            preferences[favoritePackagesKey] = validFavorites
            preferences.asMap().keys
                .filter { it.name.startsWith(RECENT_PREFIX) && it.name.removePrefix(RECENT_PREFIX) !in validPackages }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<Any>)
                }
        }
    }

    private fun toUserPreferences(preferences: Preferences): UserPreferences {
        val recent = preferences.asMap()
            .asSequence()
            .filter { (key, _) -> key.name.startsWith(RECENT_PREFIX) }
            .mapNotNull { (key, value) ->
                val timestamp = value as? Long ?: return@mapNotNull null
                key.name.removePrefix(RECENT_PREFIX) to timestamp
            }
            .toMap()
        return UserPreferences(preferences[favoritePackagesKey].orEmpty(), recent)
    }

    private fun recentKey(packageName: String): Preferences.Key<Long> =
        longPreferencesKey(RECENT_PREFIX + packageName)

    companion object {
        private const val RECENT_PREFIX = "recent_"
    }
}

data class UserPreferences(
    val favorites: Set<String> = emptySet(),
    val recentOpenedAt: Map<String, Long> = emptyMap()
)

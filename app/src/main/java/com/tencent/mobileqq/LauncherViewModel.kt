package com.tencent.mobileqq

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tencent.mobileqq.data.LauncherApp
import com.tencent.mobileqq.data.LauncherRepository
import com.tencent.mobileqq.data.PreferencesRepository
import com.tencent.mobileqq.data.SortMode
import com.tencent.mobileqq.data.UserPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LoadState {
    data object Loading : LoadState
    data object Ready : LoadState
    data class Error(val message: String) : LoadState
}

class LauncherViewModel private constructor(context: Context) : ViewModel() {
    private val repository = LauncherRepository(context)
    private val preferencesRepository = PreferencesRepository(context)
    private val appContext = context.applicationContext
    private val allApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    private val _searchQuery = MutableStateFlow("")
    private val _sortMode = MutableStateFlow(SortMode.RECENT)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var refreshJob: kotlinx.coroutines.Job? = null
    private var refreshGeneration = 0L

    val loadState: StateFlow<LoadState> = _loadState
    val searchQuery: StateFlow<String> = _searchQuery
    val sortMode: StateFlow<SortMode> = _sortMode
    val messages = _messages.asSharedFlow()
    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())
    val hasInstalledApps: StateFlow<Boolean> = allApps.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val apps: StateFlow<List<LauncherApp>> = combine(allApps, userPreferences, _searchQuery, _sortMode) { apps, preferences, query, mode ->
        com.tencent.mobileqq.data.filterAndSortApps(apps, query, preferences.favorites, preferences.recentOpenedAt, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob = viewModelScope.launch {
            _loadState.value = LoadState.Loading
            try {
                val loadedApps = repository.loadApps()
                if (generation != refreshGeneration) return@launch
                val validPackages = loadedApps.mapTo(mutableSetOf()) { it.packageName }
                preferencesRepository.removeStalePackages(validPackages)
                if (generation != refreshGeneration) return@launch
                allApps.value = loadedApps
                _loadState.value = LoadState.Ready
            } catch (exception: Exception) {
                if (exception !is kotlinx.coroutines.CancellationException) {
                    _loadState.value = LoadState.Error(exception.message ?: "无法读取应用信息")
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun toggleFavorite(app: LauncherApp) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(app.packageName)
        }
    }

    fun openApp(app: LauncherApp) {
        viewModelScope.launch {
            val intent = repository.findLaunchIntent(app.packageName)
            if (intent == null) {
                _messages.emit("无法打开${app.appName}（${app.packageName}）：找不到启动入口")
                return@launch
            }
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
                preferencesRepository.recordSuccessfulOpen(app.packageName, System.currentTimeMillis())
            } catch (exception: Exception) {
                _messages.emit("无法打开${app.appName}（${app.packageName}）：${exception.message ?: "启动失败"}")
            }
        }
    }


    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LauncherViewModel(context) as T
            }
        }
    }
}

package com.tencent.mobileqq.data

import android.graphics.drawable.Drawable

data class LauncherApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

enum class SortMode {
    RECENT,
    NAME
}

fun filterAndSortApps(
    apps: List<LauncherApp>,
    searchQuery: String,
    favorites: Set<String>,
    recentOpenedAt: Map<String, Long>,
    sortMode: SortMode,
    locale: java.util.Locale = java.util.Locale.getDefault()
): List<LauncherApp> {
    val normalizedQuery = searchQuery.trim().lowercase(java.util.Locale.ROOT)
    val collator = java.text.Collator.getInstance(locale)
    return apps.asSequence()
        .filter { app ->
            normalizedQuery.isEmpty() ||
                app.appName.lowercase(java.util.Locale.ROOT).contains(normalizedQuery) ||
                app.packageName.lowercase(java.util.Locale.ROOT).contains(normalizedQuery)
        }
        .sortedWith { first, second ->
            val favoriteComparison = (second.packageName in favorites).compareTo(first.packageName in favorites)
            if (favoriteComparison != 0) {
                favoriteComparison
            } else if (sortMode == SortMode.RECENT) {
                val firstTime = recentOpenedAt[first.packageName]
                val secondTime = recentOpenedAt[second.packageName]
                when {
                    firstTime != null && secondTime != null && firstTime != secondTime -> secondTime.compareTo(firstTime)
                    firstTime != null && secondTime == null -> -1
                    firstTime == null && secondTime != null -> 1
                    else -> compareNames(first, second, collator)
                }
            } else {
                compareNames(first, second, collator)
            }
        }
        .toList()
}

private fun compareNames(first: LauncherApp, second: LauncherApp, collator: java.text.Collator): Int {
    val nameComparison = collator.compare(first.appName, second.appName)
    return if (nameComparison != 0) nameComparison else first.packageName.compareTo(second.packageName)
}

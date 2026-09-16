package com.tencent.mobileqq

import com.tencent.mobileqq.data.LauncherApp
import com.tencent.mobileqq.data.SortMode
import com.tencent.mobileqq.data.filterAndSortApps
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LauncherSortingTest {
    private val apps = listOf(
        LauncherApp("com.zeta", "Zeta", null),
        LauncherApp("com.alpha", "Alpha", null),
        LauncherApp("com.beta", "Beta", null)
    )

    @Test
    fun searchMatchesNameAndPackageWithoutCase() {
        assertEquals(listOf("com.alpha"), filterAndSortApps(apps, "ALPHA", emptySet(), emptyMap(), SortMode.NAME).map { it.packageName })
        assertEquals(listOf("com.beta"), filterAndSortApps(apps, "COM.BETA", emptySet(), emptyMap(), SortMode.NAME).map { it.packageName })
    }

    @Test
    fun favoritesAlwaysComeFirst() {
        val result = filterAndSortApps(apps, "", setOf("com.zeta"), mapOf("com.alpha" to 20L), SortMode.RECENT)
        assertEquals("com.zeta", result.first().packageName)
    }

    @Test
    fun recentItemsSortByNewestThenUnopenedByName() {
        val result = filterAndSortApps(apps, "", emptySet(), mapOf("com.zeta" to 10L, "com.beta" to 30L), SortMode.RECENT)
        assertEquals(listOf("com.beta", "com.zeta", "com.alpha"), result.map { it.packageName })
    }

    @Test
    fun nameSortUsesLocaleCollatorAndPackageAsStableTieBreaker() {
        val sameNames = listOf(
            LauncherApp("com.two", "相同", null),
            LauncherApp("com.one", "相同", null),
            LauncherApp("com.z", "阿", null)
        )
        val result = filterAndSortApps(sameNames, "", emptySet(), emptyMap(), SortMode.NAME, Locale.CHINA)
        assertEquals(listOf("com.z", "com.one", "com.two"), result.map { it.packageName })
    }
}

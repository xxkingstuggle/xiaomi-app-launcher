package com.tencent.mobileqq.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherRepository(context: Context) {
    private val packageManager = context.packageManager

    suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .filter { it.packageName != APPLICATION_ID }
            .distinctBy { it.packageName }
            .map { applicationInfo ->
                LauncherApp(
                    packageName = applicationInfo.packageName,
                    appName = applicationInfo.loadLabel(packageManager).toString().ifBlank { applicationInfo.packageName },
                    icon = applicationInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.packageName }
            .toList()
    }

    suspend fun findLaunchIntent(packageName: String): Intent? = withContext(Dispatchers.IO) {
        packageManager.getLaunchIntentForPackage(packageName)
    }

    companion object {
        const val APPLICATION_ID = "com.tencent.mobileqq"
    }
}

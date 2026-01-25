package com.ross.livemedia.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.ross.livemedia.storage.StorageHelper

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isEnabled: Boolean
)

class AppRepository(
    private val context: Context,
    private val storageHelper: StorageHelper
) {
    private val packageManager = context.packageManager

    fun getAllApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolvedInfoList = packageManager.queryIntentActivities(launcherIntent, 0)
        
        return resolvedInfoList
            .mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    // Avoid adding our own app if desired, specifically checking against context.packageName
                    if (packageName == context.packageName) return@mapNotNull null

                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    val icon = resolveInfo.loadIcon(packageManager)
                    
                    AppInfo(
                        name = appName,
                        packageName = packageName,
                        icon = icon,
                        isEnabled = storageHelper.isAppEnabled(packageName)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        storageHelper.setAppEnabled(packageName, enabled)
    }

    fun hasAppEnabled(packageName: String): Boolean {
        return storageHelper.isAppEnabled(packageName)
    }
    
    fun setAllAppsEnabled(apps: List<AppInfo>, enabled: Boolean) {
        val packageNames = apps.map { it.packageName }
        storageHelper.setAllAppsEnabled(packageNames, enabled)
    }
}

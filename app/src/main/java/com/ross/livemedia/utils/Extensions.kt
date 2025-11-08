package com.ross.livemedia.utils

import android.content.pm.PackageManager

fun PackageManager.getAppName(pkg: String): CharSequence {
    return try {
        getApplicationLabel(getApplicationInfo(pkg, 0))
    } catch (e: Exception) {
        "Unknown App"
    }
}
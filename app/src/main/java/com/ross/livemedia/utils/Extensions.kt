package com.ross.livemedia.utils

import android.content.pm.PackageManager

fun PackageManager.getAppName(pkg: String) = getApplicationLabel(getApplicationInfo(pkg, 0))
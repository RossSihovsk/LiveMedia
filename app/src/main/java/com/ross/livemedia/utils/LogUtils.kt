package com.ross.livemedia.utils

import android.util.Log

class Logger(private val tag: String) {

    fun debug(message: String) {
        Log.d(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
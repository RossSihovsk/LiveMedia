package com.ross.livemedia.qs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MediaAppRecentsProvider {
    private val _mediaAppClosedFromRecents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mediaAppClosedFromRecents = _mediaAppClosedFromRecents.asSharedFlow()

    private val _mediaAppReopened = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mediaAppReopened = _mediaAppReopened.asSharedFlow()

    @Volatile
    var trackedPackage: String? = null

    fun onMediaAppClosedFromRecents(packageName: String) {
        _mediaAppClosedFromRecents.tryEmit(packageName)
    }

    fun onMediaAppReopened(packageName: String) {
        _mediaAppReopened.tryEmit(packageName)
    }
}
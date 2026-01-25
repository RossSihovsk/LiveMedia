package com.ross.livemedia.allowed_apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ross.livemedia.data.repository.AppInfo
import com.ross.livemedia.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllowedAppsViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _uiState = MutableStateFlow<List<AppInfo>>(emptyList())
    val uiState: StateFlow<List<AppInfo>> = _uiState

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadApps()
        observeSearch()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                appRepository.getAllApps()
            }
            _allApps.value = apps
            _uiState.value = apps
            _isLoading.value = false
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            combine(_allApps, _searchQuery) { apps, query ->
                if (query.isBlank()) {
                    apps
                } else {
                    apps.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.packageName.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filteredApps ->
                _uiState.value = filteredApps
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        appRepository.setAppEnabled(packageName, enabled)

        // Update local state to reflect change immediately in UI
        val updateList = { list: List<AppInfo> ->
            list.map {
                if (it.packageName == packageName) it.copy(isEnabled = enabled) else it
            }
        }

        _allApps.value = updateList(_allApps.value)
        // trigger recomposition via search flow automatically or update manually?
        // combine will emit new value because _allApps changed.
    }

    fun setAllEnabled(enabled: Boolean) {
        val currentList =
            _uiState.value // Only affect currently visible apps (filtered) or all apps?
        // Usually "Enable All" logic applies to visible items in list if filtered, or global.
        // Let's assume global "All" means all visible/search results or just all apps?
        // User request: "The viewModel should return list of all apps on device."
        // "buttons disable/enable all"

        // If I have a search filter active, "Enable All" usually means "Enable all visible results".
        // But to be safe and consistent with previous behavior, let's affect *visible* apps.

        appRepository.setAllAppsEnabled(currentList, enabled)

        // Update state
        val updatedPackageNames = currentList.map { it.packageName }.toSet()
        val updateList = { list: List<AppInfo> ->
            list.map {
                if (updatedPackageNames.contains(it.packageName)) it.copy(isEnabled = enabled) else it
            }
        }
        _allApps.value = updateList(_allApps.value)
    }
}
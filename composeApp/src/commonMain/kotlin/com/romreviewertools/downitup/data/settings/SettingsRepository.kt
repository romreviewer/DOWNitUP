package com.romreviewertools.downitup.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing app settings using multiplatform-settings library
 */
class SettingsRepository(
    private val settings: Settings
) {
    companion object {
        private const val KEY_DOWNLOAD_LOCATION = "download_location"
        private const val KEY_USE_MULTI_CONNECTION_DEFAULT = "use_multi_connection_default"
        private const val KEY_DEFAULT_CONNECTION_COUNT = "default_connection_count"
        private const val KEY_STORAGE_PERMISSION_GRANTED = "storage_permission_granted"
    }

    private val _downloadLocation = MutableStateFlow<String?>(null)
    val downloadLocation: StateFlow<String?> = _downloadLocation.asStateFlow()

    private val _useMultiConnectionDefault = MutableStateFlow(true)
    val useMultiConnectionDefault: StateFlow<Boolean> = _useMultiConnectionDefault.asStateFlow()

    private val _defaultConnectionCount = MutableStateFlow(4)
    val defaultConnectionCount: StateFlow<Int> = _defaultConnectionCount.asStateFlow()

    private val _storagePermissionGranted = MutableStateFlow(false)
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted.asStateFlow()

    init {
        // Load initial values
        _downloadLocation.value = settings.getStringOrNull(KEY_DOWNLOAD_LOCATION)
        _useMultiConnectionDefault.value = settings.getBoolean(KEY_USE_MULTI_CONNECTION_DEFAULT, true)
        _defaultConnectionCount.value = settings.getInt(KEY_DEFAULT_CONNECTION_COUNT, 4)
        _storagePermissionGranted.value = settings.getBoolean(KEY_STORAGE_PERMISSION_GRANTED, false)
    }

    fun setDownloadLocation(path: String?) {
        if (path != null) {
            settings[KEY_DOWNLOAD_LOCATION] = path
        } else {
            settings.remove(KEY_DOWNLOAD_LOCATION)
        }
        _downloadLocation.value = path
    }

    fun getDownloadLocation(): String? {
        return settings.getStringOrNull(KEY_DOWNLOAD_LOCATION)
    }

    fun setUseMultiConnectionDefault(enabled: Boolean) {
        settings[KEY_USE_MULTI_CONNECTION_DEFAULT] = enabled
        _useMultiConnectionDefault.value = enabled
    }

    fun setDefaultConnectionCount(count: Int) {
        settings[KEY_DEFAULT_CONNECTION_COUNT] = count
        _defaultConnectionCount.value = count
    }

    fun setStoragePermissionGranted(granted: Boolean) {
        settings[KEY_STORAGE_PERMISSION_GRANTED] = granted
        _storagePermissionGranted.value = granted
    }

    fun isStoragePermissionGranted(): Boolean {
        return settings.getBoolean(KEY_STORAGE_PERMISSION_GRANTED, false)
    }
}

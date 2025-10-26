package com.romreviewertools.downitup.util

/**
 * Platform-specific permission handler interface
 */
interface PermissionHandler {
    /**
     * Check if storage permission is granted
     */
    suspend fun isStoragePermissionGranted(): Boolean

    /**
     * Request storage permission
     * Returns true if granted, false otherwise
     */
    suspend fun requestStoragePermission(): Boolean

    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings()
}

/**
 * Create platform-specific permission handler
 */
expect fun createPermissionHandler(): PermissionHandler

package com.romreviewertools.downitup.util

/**
 * JVM/Desktop implementation - no permissions needed
 */
class JvmPermissionHandler : PermissionHandler {
    override suspend fun isStoragePermissionGranted(): Boolean {
        // Desktop platforms don't require storage permissions
        return true
    }

    override suspend fun requestStoragePermission(): Boolean {
        // Desktop platforms don't require storage permissions
        return true
    }

    override fun openAppSettings() {
        // No-op for desktop
    }
}

actual fun createPermissionHandler(): PermissionHandler = JvmPermissionHandler()

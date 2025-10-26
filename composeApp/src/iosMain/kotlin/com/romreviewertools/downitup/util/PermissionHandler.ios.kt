package com.romreviewertools.downitup.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS implementation for permission handling
 * Note: iOS file storage in app directory doesn't require special permissions
 */
class IosPermissionHandler : PermissionHandler {
    override suspend fun isStoragePermissionGranted(): Boolean {
        // iOS apps have access to their Documents directory by default
        return true
    }

    override suspend fun requestStoragePermission(): Boolean {
        // iOS apps have access to their Documents directory by default
        return true
    }

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
            UIApplication.sharedApplication.openURL(settingsUrl)
        }
    }
}

actual fun createPermissionHandler(): PermissionHandler = IosPermissionHandler()

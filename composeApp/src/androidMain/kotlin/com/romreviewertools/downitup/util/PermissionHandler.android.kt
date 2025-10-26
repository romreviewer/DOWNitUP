package com.romreviewertools.downitup.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private lateinit var appContext: Context

fun initializePermissionHandler(context: Context) {
    appContext = context.applicationContext
}

/**
 * Android implementation for storage permission handling
 * Handles different Android versions and their permission requirements
 */
class AndroidPermissionHandler(
    private val context: Context = appContext
) : PermissionHandler {

    override suspend fun isStoragePermissionGranted(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ (API 30+) - Use MANAGE_EXTERNAL_STORAGE or scoped storage
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10 - Use WRITE_EXTERNAL_STORAGE
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Below Android 6 - permissions granted at install time
                true
            }
        }
    }

    override suspend fun requestStoragePermission(): Boolean {
        // For now, return the current permission status
        // Full implementation would need activity context for requesting permissions
        return isStoragePermissionGranted()
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    companion object {
        /**
         * Request storage permission from an Activity
         * This should be called from Compose using Accompanist Permissions
         */
        fun getRequiredPermissions(): List<String> {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13+ - Use media permissions
                    listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11-12 - Request scoped storage permissions
                    listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                else -> {
                    // Android 6-10
                    listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
        }
    }
}

actual fun createPermissionHandler(): PermissionHandler = AndroidPermissionHandler()

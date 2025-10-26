package com.romreviewertools.downitup.data.settings

import android.content.Context
import androidx.preference.PreferenceManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private lateinit var appContext: Context

fun initializeSettings(context: Context) {
    appContext = context.applicationContext
}

actual fun createSettings(): Settings {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    return SharedPreferencesSettings(sharedPreferences)
}

package com.romreviewertools.downitup.data.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createSettings(): Settings {
    val preferences = Preferences.userRoot().node("com.romreviewertools.downitup")
    return PreferencesSettings(preferences)
}

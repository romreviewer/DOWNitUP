package com.romreviewertools.downitup.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Downloads : Screen("downloads", "Downloads", Icons.Default.CloudDownload)
    data object Browser : Screen("browser", "Browser", Icons.Default.TravelExplore)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val mainScreens = listOf(Downloads, Browser, Settings)
    }
}

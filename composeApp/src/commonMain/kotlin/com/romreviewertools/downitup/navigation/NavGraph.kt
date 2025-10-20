package com.romreviewertools.downitup.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.romreviewertools.downitup.ui.browser.BrowserScreen
import com.romreviewertools.downitup.ui.downloads.DownloadsScreen
import com.romreviewertools.downitup.ui.downloads.DownloadsViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: DownloadsViewModel? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Downloads.route
    ) {
        composable(Screen.Downloads.route) {
            DownloadsScreen(viewModel)
        }
        composable(Screen.Browser.route) {
            BrowserScreen(viewModel)
        }
    }
}

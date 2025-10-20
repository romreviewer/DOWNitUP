package com.romreviewertools.downitup

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.romreviewertools.downitup.data.local.DatabaseDriverFactory
import com.romreviewertools.downitup.di.AppDependencies

fun main() = application {
    // Initialize dependencies
    val dependencies = AppDependencies(DatabaseDriverFactory())

    Window(
        onCloseRequest = {
            dependencies.shutdown()
            exitApplication()
        },
        title = "DOWNitUP",
    ) {
        App(dependencies)
    }
}
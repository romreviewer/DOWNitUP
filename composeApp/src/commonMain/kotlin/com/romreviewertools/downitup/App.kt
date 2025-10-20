package com.romreviewertools.downitup

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.romreviewertools.downitup.di.AppDependencies
import com.romreviewertools.downitup.ui.MainScreen

@Composable
@Preview
fun App(dependencies: AppDependencies? = null) {
    MaterialTheme {
        MainScreen(dependencies?.downloadsViewModel)
    }
}
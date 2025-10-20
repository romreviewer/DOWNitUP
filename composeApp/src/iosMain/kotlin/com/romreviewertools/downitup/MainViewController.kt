package com.romreviewertools.downitup

import androidx.compose.ui.window.ComposeUIViewController
import com.romreviewertools.downitup.data.local.DatabaseDriverFactory
import com.romreviewertools.downitup.di.AppDependencies

private val dependencies = AppDependencies(DatabaseDriverFactory())

fun MainViewController() = ComposeUIViewController { App(dependencies) }
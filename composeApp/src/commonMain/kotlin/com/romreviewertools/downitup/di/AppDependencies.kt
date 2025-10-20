package com.romreviewertools.downitup.di

import com.romreviewertools.downitup.data.file.FileWriter
import com.romreviewertools.downitup.data.file.createFileWriter
import com.romreviewertools.downitup.data.local.AppDatabase
import com.romreviewertools.downitup.data.local.DatabaseDriverFactory
import com.romreviewertools.downitup.data.local.createDatabase
import com.romreviewertools.downitup.data.manager.HttpDownloadManager
import com.romreviewertools.downitup.data.repository.DownloadRepositoryImpl
import com.romreviewertools.downitup.domain.manager.DownloadManager
import com.romreviewertools.downitup.domain.repository.DownloadRepository
import com.romreviewertools.downitup.ui.downloads.DownloadsViewModel
import io.ktor.client.*

/**
 * Simple dependency container for the app
 * TODO: Replace with proper DI (Koin) in production
 */
class AppDependencies(databaseDriverFactory: DatabaseDriverFactory) {

    // Database
    val database: AppDatabase = createDatabase(databaseDriverFactory)

    // File Writer
    val fileWriter: FileWriter = createFileWriter()

    // HTTP Client - platform-specific implementation
    val httpClient: HttpClient = createHttpClient()

    // Repository
    val repository: DownloadRepository = DownloadRepositoryImpl(database)

    // Download Manager
    val downloadManager: DownloadManager = HttpDownloadManager(
        httpClient = httpClient,
        database = database,
        fileWriter = fileWriter
    )

    // ViewModels
    val downloadsViewModel: DownloadsViewModel = DownloadsViewModel(
        repository = repository,
        downloadManager = downloadManager
    )

    /**
     * Clean up resources when app closes
     */
    fun shutdown() {
        (downloadManager as? HttpDownloadManager)?.shutdown()
        httpClient.close()
    }
}

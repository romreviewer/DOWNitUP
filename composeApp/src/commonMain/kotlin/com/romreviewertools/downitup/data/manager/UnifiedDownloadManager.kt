package com.romreviewertools.downitup.data.manager

import com.romreviewertools.downitup.data.local.AppDatabase
import com.romreviewertools.downitup.data.local.model.DownloadType
import com.romreviewertools.downitup.data.torrent.TorrentDownloadManager
import com.romreviewertools.downitup.domain.manager.DownloadManager
import com.romreviewertools.downitup.domain.model.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Unified download manager that routes operations to the appropriate manager
 * based on download type (HTTP or TORRENT)
 */
class UnifiedDownloadManager(
    private val httpManager: HttpDownloadManager,
    private val torrentManager: TorrentDownloadManager,
    private val database: AppDatabase
) : DownloadManager {

    override suspend fun startDownload(downloadId: Long) {
        val download = getDownloadType(downloadId)
        when (download?.downloadType) {
            DownloadType.HTTP -> httpManager.startDownload(downloadId)
            DownloadType.TORRENT -> torrentManager.startDownload(downloadId)
            null -> println("UnifiedDownloadManager: Download $downloadId not found")
        }
    }

    override suspend fun pauseDownload(downloadId: Long) {
        val download = getDownloadType(downloadId)
        when (download?.downloadType) {
            DownloadType.HTTP -> httpManager.pauseDownload(downloadId)
            DownloadType.TORRENT -> torrentManager.pauseDownload(downloadId)
            null -> println("UnifiedDownloadManager: Download $downloadId not found")
        }
    }

    override suspend fun cancelDownload(downloadId: Long) {
        val download = getDownloadType(downloadId)
        when (download?.downloadType) {
            DownloadType.HTTP -> httpManager.cancelDownload(downloadId)
            DownloadType.TORRENT -> torrentManager.cancelDownload(downloadId)
            null -> println("UnifiedDownloadManager: Download $downloadId not found")
        }
    }

    override suspend fun deleteDownload(downloadId: Long) {
        val download = getDownloadType(downloadId)
        when (download?.downloadType) {
            DownloadType.HTTP -> httpManager.deleteDownload(downloadId)
            DownloadType.TORRENT -> torrentManager.deleteDownload(downloadId)
            null -> println("UnifiedDownloadManager: Download $downloadId not found")
        }
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Download> {
        // For now, only HTTP manager implements progress flow
        // Torrent manager would need to be updated to provide progress
        return httpManager.getDownloadProgress(downloadId)
    }

    override fun isDownloadActive(downloadId: Long): Boolean {
        return httpManager.isDownloadActive(downloadId) ||
               torrentManager.isDownloadActive(downloadId)
    }

    /**
     * Get download type from database
     */
    private suspend fun getDownloadType(downloadId: Long): DownloadTypeInfo? {
        return try {
            val downloadEntity = database.downloadQueries.getDownloadById(downloadId)
                .executeAsOneOrNull()
            downloadEntity?.let {
                DownloadTypeInfo(
                    downloadType = DownloadType.valueOf(it.downloadType)
                )
            }
        } catch (e: Exception) {
            println("UnifiedDownloadManager: Error getting download type: ${e.message}")
            null
        }
    }

    private data class DownloadTypeInfo(
        val downloadType: DownloadType
    )

    /**
     * Shutdown all managers
     */
    fun shutdown() {
        httpManager.shutdown()
        // torrentManager.shutdown() // If torrent manager had shutdown method
    }
}

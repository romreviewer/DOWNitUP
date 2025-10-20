package com.romreviewertools.downitup.domain.manager

import com.romreviewertools.downitup.domain.model.Download
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing downloads (HTTP and Torrent)
 */
interface DownloadManager {
    /**
     * Start or resume a download
     */
    suspend fun startDownload(downloadId: Long)

    /**
     * Pause a download
     */
    suspend fun pauseDownload(downloadId: Long)

    /**
     * Cancel a download and delete partial files
     */
    suspend fun cancelDownload(downloadId: Long)

    /**
     * Delete a completed download
     */
    suspend fun deleteDownload(downloadId: Long)

    /**
     * Get real-time progress updates for a download
     */
    fun getDownloadProgress(downloadId: Long): Flow<Download>

    /**
     * Check if a download is currently active
     */
    fun isDownloadActive(downloadId: Long): Boolean
}

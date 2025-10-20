package com.romreviewertools.downitup.domain.repository

import com.romreviewertools.downitup.data.local.model.DownloadStatus
import com.romreviewertools.downitup.data.local.model.DownloadType
import com.romreviewertools.downitup.domain.model.Download
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    // Observe downloads
    fun getAllDownloads(): Flow<List<Download>>

    fun getActiveDownloads(): Flow<List<Download>>

    fun getCompletedDownloads(): Flow<List<Download>>

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>>

    // Single download operations
    suspend fun getDownloadById(id: Long): Download?

    // Add downloads
    suspend fun addHttpDownload(
        url: String,
        name: String,
        savePath: String
    ): Long

    suspend fun addTorrentDownload(
        magnetUri: String,
        name: String,
        savePath: String,
        infoHash: String? = null
    ): Long

    // Manage downloads
    suspend fun updateDownload(download: Download)

    suspend fun updateDownloadProgress(
        id: Long,
        downloadedBytes: Long,
        downloadSpeed: Long
    )

    suspend fun updateDownloadStatus(
        id: Long,
        status: DownloadStatus,
        error: String? = null
    )

    suspend fun deleteDownload(id: Long, deleteFiles: Boolean = false)

    suspend fun deleteAllDownloads()

    // Statistics
    suspend fun getDownloadCount(): Int

    suspend fun getDownloadCountByStatus(status: DownloadStatus): Int
}

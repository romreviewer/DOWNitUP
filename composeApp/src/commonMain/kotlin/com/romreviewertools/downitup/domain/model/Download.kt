package com.romreviewertools.downitup.domain.model

import com.romreviewertools.downitup.data.local.model.DownloadStatus
import com.romreviewertools.downitup.data.local.model.DownloadType
import com.romreviewertools.downitup.data.local.Downloads

data class Download(
    val id: Long,
    val name: String,
    val url: String,
    val downloadType: DownloadType,
    val status: DownloadStatus,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val downloadSpeed: Long,
    val savePath: String,
    val mimeType: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val error: String?,
    val infoHash: String?,
    val seeders: Int,
    val peers: Int,
    val magnetUri: String?,
    val connectionCount: Int,
    val chunkedDownload: Boolean,
    val chunks: List<DownloadChunk> = emptyList()
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat())
        } else 0f

    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED

    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.QUEUED

    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED
}

// Extension functions to map between SQLDelight model and domain model
fun Downloads.toDomain(chunks: List<DownloadChunk> = emptyList()): Download {
    return Download(
        id = id,
        name = name,
        url = url,
        downloadType = DownloadType.valueOf(downloadType),
        status = DownloadStatus.valueOf(status),
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        downloadSpeed = downloadSpeed,
        savePath = savePath,
        mimeType = mimeType,
        createdAt = createdAt,
        completedAt = completedAt,
        error = error,
        infoHash = infoHash,
        seeders = seeders.toInt(),
        peers = peers.toInt(),
        magnetUri = magnetUri,
        connectionCount = connectionCount.toInt(),
        chunkedDownload = chunkedDownload != 0L,
        chunks = chunks
    )
}

package com.romreviewertools.downitup.domain.model

import com.romreviewertools.downitup.data.local.Download_chunks
import com.romreviewertools.downitup.data.local.model.ChunkStatus

/**
 * Represents a single chunk/connection in a multi-connection download
 */
data class DownloadChunk(
    val id: Long,
    val downloadId: Long,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long,
    val status: ChunkStatus,
    val speed: Long
) {
    /**
     * Progress of this chunk (0.0 to 1.0)
     */
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f

    /**
     * Total bytes in this chunk
     */
    val totalBytes: Long
        get() = endByte - startByte + 1

    /**
     * Remaining bytes to download in this chunk
     */
    val remainingBytes: Long
        get() = totalBytes - downloadedBytes

    /**
     * Is this chunk actively downloading
     */
    val isActive: Boolean
        get() = status == ChunkStatus.DOWNLOADING

    /**
     * Is this chunk completed
     */
    val isCompleted: Boolean
        get() = status == ChunkStatus.COMPLETED
}

/**
 * Extension function to map SQLDelight model to domain model
 */
fun Download_chunks.toDomain(): DownloadChunk {
    return DownloadChunk(
        id = id,
        downloadId = downloadId,
        chunkIndex = chunkIndex.toInt(),
        startByte = startByte,
        endByte = endByte,
        downloadedBytes = downloadedBytes,
        status = ChunkStatus.valueOf(status),
        speed = speed
    )
}

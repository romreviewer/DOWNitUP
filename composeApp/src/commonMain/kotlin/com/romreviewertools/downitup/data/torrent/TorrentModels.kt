package com.romreviewertools.downitup.data.torrent

import kotlinx.serialization.Serializable

/**
 * Torrent progress information
 */
@Serializable
data class TorrentProgress(
    val downloadId: Long,
    val progress: Float, // 0.0 to 1.0
    val downloadSpeed: Long, // bytes per second
    val uploadSpeed: Long, // bytes per second
    val downloadedBytes: Long,
    val totalBytes: Long,
    val numSeeds: Int,
    val numPeers: Int,
    val state: TorrentState
)

/**
 * Torrent download states
 */
enum class TorrentState {
    CHECKING_FILES,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    FINISHED,
    SEEDING,
    ALLOCATING,
    CHECKING_RESUME_DATA
}

/**
 * Torrent metadata parsed from magnet link or torrent file
 */
data class TorrentMetadata(
    val infoHash: String,
    val name: String?,
    val totalSize: Long,
    val trackers: List<String> = emptyList()
)

/**
 * Result of torrent detection/parsing
 */
sealed class TorrentDetectionResult {
    data class MagnetLink(val metadata: TorrentMetadata) : TorrentDetectionResult()
    data class TorrentFile(val metadata: TorrentMetadata) : TorrentDetectionResult()
    data object NotTorrent : TorrentDetectionResult()
    data class Error(val message: String) : TorrentDetectionResult()
}

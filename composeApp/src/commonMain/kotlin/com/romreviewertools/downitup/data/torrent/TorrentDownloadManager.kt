package com.romreviewertools.downitup.data.torrent

import com.romreviewertools.downitup.domain.manager.DownloadManager
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific torrent download manager
 *
 * Implementations:
 * - Android: TorrentStream-Android (best streaming support)
 * - Desktop (JVM): Anitorrent (KMP support)
 * - iOS: Custom implementation (to be completed later)
 */
expect class TorrentDownloadManager() : DownloadManager {

    /**
     * Add a torrent download from a magnet link
     * @return info hash of the added torrent
     */
    suspend fun addMagnetLink(
        downloadId: Long,
        magnetUri: String,
        savePath: String
    ): String

    /**
     * Add a torrent download from a .torrent file
     * @return info hash of the added torrent
     */
    suspend fun addTorrentFile(
        downloadId: Long,
        torrentFilePath: String,
        savePath: String
    ): String

    /**
     * Enable/disable sequential download (for streaming)
     */
    suspend fun setSequentialDownload(downloadId: Long, enabled: Boolean)

    /**
     * Get torrent-specific progress information
     */
    fun getTorrentProgress(downloadId: Long): Flow<TorrentProgress>

    /**
     * Parse a magnet link to extract metadata
     */
    fun parseMagnetLink(magnetUri: String): TorrentMetadata

    /**
     * Check if a URL is a magnet link
     */
    fun isMagnetLink(url: String): Boolean
}

/**
 * Helper function to detect if a URL/string is a torrent
 */
fun detectTorrentType(input: String): TorrentDetectionResult {
    return when {
        input.trim().startsWith("magnet:?", ignoreCase = true) -> {
            try {
                // TODO: Parse magnet link properly
                val infoHashMatch = Regex("xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})")
                    .find(input)
                val infoHash = infoHashMatch?.groupValues?.get(1) ?: ""

                val nameMatch = Regex("dn=([^&]+)").find(input)
                val name = nameMatch?.groupValues?.get(1)?.replace("+", " ")

                TorrentDetectionResult.MagnetLink(
                    TorrentMetadata(
                        infoHash = infoHash,
                        name = name,
                        totalSize = 0L // Not available in magnet until metadata is downloaded
                    )
                )
            } catch (e: Exception) {
                TorrentDetectionResult.Error("Failed to parse magnet link: ${e.message}")
            }
        }
        input.endsWith(".torrent", ignoreCase = true) -> {
            // TODO: Parse .torrent file
            TorrentDetectionResult.Error("Torrent file parsing not yet implemented")
        }
        else -> TorrentDetectionResult.NotTorrent
    }
}

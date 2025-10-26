package com.romreviewertools.downitup.data.torrent

import com.romreviewertools.downitup.domain.model.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS implementation of TorrentDownloadManager
 *
 * TODO: Implement using SwiftyTorrent or custom libtorrent bridge
 * For MVP, torrent support on iOS is deferred to future versions.
 */
actual class TorrentDownloadManager actual constructor() :
    com.romreviewertools.downitup.domain.manager.DownloadManager {

    actual suspend fun addMagnetLink(
        downloadId: Long,
        magnetUri: String,
        savePath: String
    ): String {
        throw NotImplementedError(
            "Torrent downloads are not yet supported on iOS. " +
            "Please use HTTP downloads or switch to Android/Desktop for torrent support."
        )
    }

    actual suspend fun addTorrentFile(
        downloadId: Long,
        torrentFilePath: String,
        savePath: String
    ): String {
        throw NotImplementedError(
            "Torrent downloads are not yet supported on iOS. " +
            "Please use HTTP downloads or switch to Android/Desktop for torrent support."
        )
    }

    actual suspend fun setSequentialDownload(downloadId: Long, enabled: Boolean) {
        throw NotImplementedError("Torrent downloads are not yet supported on iOS.")
    }

    actual fun getTorrentProgress(downloadId: Long): Flow<TorrentProgress> {
        return emptyFlow()
    }

    actual fun parseMagnetLink(magnetUri: String): TorrentMetadata {
        // Basic parsing without actual torrent support
        val infoHashMatch = Regex("xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})")
            .find(magnetUri)
        val infoHash = infoHashMatch?.groupValues?.get(1) ?: ""

        val nameMatch = Regex("dn=([^&]+)").find(magnetUri)
        val name = nameMatch?.groupValues?.get(1)?.replace("+", " ")

        return TorrentMetadata(
            infoHash = infoHash,
            name = name,
            totalSize = 0L
        )
    }

    actual fun isMagnetLink(url: String): Boolean {
        return url.trim().startsWith("magnet:?", ignoreCase = true)
    }

    override suspend fun startDownload(downloadId: Long) {
        throw NotImplementedError("Torrent downloads are not yet supported on iOS.")
    }

    override suspend fun pauseDownload(downloadId: Long) {
        throw NotImplementedError("Torrent downloads are not yet supported on iOS.")
    }

    override suspend fun cancelDownload(downloadId: Long) {
        throw NotImplementedError("Torrent downloads are not yet supported on iOS.")
    }

    override suspend fun deleteDownload(downloadId: Long) {
        throw NotImplementedError("Torrent downloads are not yet supported on iOS.")
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Download> {
        return emptyFlow()
    }

    override fun isDownloadActive(downloadId: Long): Boolean {
        return false
    }
}

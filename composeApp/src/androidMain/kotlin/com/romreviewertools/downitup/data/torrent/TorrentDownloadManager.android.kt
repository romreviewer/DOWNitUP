package com.romreviewertools.downitup.data.torrent

import android.content.Context
import com.github.se.torrentstreamserver.Torrent
import com.github.se.torrentstreamserver.TorrentListener
import com.github.se.torrentstreamserver.TorrentOptions
import com.github.se.torrentstreamserver.TorrentStream
import com.github.se.torrentstreamserver.StreamStatus
import com.romreviewertools.downitup.domain.model.Download
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Android implementation of TorrentDownloadManager using TorrentStream-Android
 *
 * Note: Requires Context for initialization. Use the factory function to create instances.
 */
actual class TorrentDownloadManager private constructor(
    private val torrentStream: TorrentStream
) : com.romreviewertools.downitup.domain.manager.DownloadManager {

    companion object {
        /**
         * Factory function to create TorrentDownloadManager with Android Context
         */
        fun create(context: Context, downloadDir: File): TorrentDownloadManager {
            val options = TorrentOptions.Builder()
                .saveLocation(downloadDir)
                .removeFilesAfterStop(false)
                .autoDownload(true)
                .build()

            val torrentStream = TorrentStream.init(options)
            return TorrentDownloadManager(torrentStream)
        }
    }

    // Required for expect/actual - will throw if called without Context
    actual constructor() : this(createDummyStream())

    private val activeTorrents = mutableMapOf<Long, Torrent>()
    private val progressFlows = mutableMapOf<Long, MutableStateFlow<Download?>>()
    private val torrentProgressFlows = mutableMapOf<Long, MutableStateFlow<TorrentProgress?>>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    actual suspend fun addMagnetLink(
        downloadId: Long,
        magnetUri: String,
        savePath: String
    ): String = withContext(Dispatchers.Main) {
        val completableDeferred = CompletableDeferred<String>()

        torrentStream.addListener(object : TorrentListener {
            override fun onStreamReady(torrent: Torrent) {
                activeTorrents[downloadId] = torrent
                val infoHash = extractInfoHashFromMagnet(magnetUri)
                completableDeferred.complete(infoHash)
                startProgressMonitoring(downloadId, torrent)
            }

            override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
                // Handled in progress monitoring
            }

            override fun onStreamStopped() {
                // Torrent stopped
            }

            override fun onStreamPrepared(torrent: Torrent) {
                // Preparation complete
            }

            override fun onStreamStarted(torrent: Torrent) {
                // Stream started
            }

            override fun onStreamError(torrent: Torrent?, e: Exception?) {
                if (!completableDeferred.isCompleted) {
                    completableDeferred.completeExceptionally(
                        e ?: Exception("Unknown torrent error")
                    )
                }
            }
        })

        torrentStream.startStream(magnetUri)
        return@withContext completableDeferred.await()
    }

    actual suspend fun addTorrentFile(
        downloadId: Long,
        torrentFilePath: String,
        savePath: String
    ): String = withContext(Dispatchers.Main) {
        // TorrentStream also supports .torrent files
        val completableDeferred = CompletableDeferred<String>()

        torrentStream.addListener(object : TorrentListener {
            override fun onStreamReady(torrent: Torrent) {
                activeTorrents[downloadId] = torrent
                // Extract info hash from torrent file
                val infoHash = "" // TODO: Parse from torrent file
                completableDeferred.complete(infoHash)
                startProgressMonitoring(downloadId, torrent)
            }

            override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {}
            override fun onStreamStopped() {}
            override fun onStreamPrepared(torrent: Torrent) {}
            override fun onStreamStarted(torrent: Torrent) {}

            override fun onStreamError(torrent: Torrent?, e: Exception?) {
                if (!completableDeferred.isCompleted) {
                    completableDeferred.completeExceptionally(
                        e ?: Exception("Unknown torrent error")
                    )
                }
            }
        })

        torrentStream.startStream(torrentFilePath)
        return@withContext completableDeferred.await()
    }

    actual suspend fun setSequentialDownload(downloadId: Long, enabled: Boolean) {
        // TorrentStream automatically handles sequential download for streaming
        // No explicit API call needed
    }

    actual fun getTorrentProgress(downloadId: Long): Flow<TorrentProgress> {
        return torrentProgressFlows.getOrPut(downloadId) {
            MutableStateFlow(null)
        }.filterNotNull()
    }

    actual fun parseMagnetLink(magnetUri: String): TorrentMetadata {
        val infoHash = extractInfoHashFromMagnet(magnetUri)

        val nameMatch = Regex("dn=([^&]+)").find(magnetUri)
        val name = nameMatch?.groupValues?.get(1)?.replace("+", " ")

        val trackers = Regex("tr=([^&]+)")
            .findAll(magnetUri)
            .map { it.groupValues[1] }
            .toList()

        return TorrentMetadata(
            infoHash = infoHash,
            name = name,
            totalSize = 0L,
            trackers = trackers
        )
    }

    actual fun isMagnetLink(url: String): Boolean {
        return url.trim().startsWith("magnet:?", ignoreCase = true)
    }

    override suspend fun startDownload(downloadId: Long) {
        val torrent = activeTorrents[downloadId] ?: return
        torrent.resume()
    }

    override suspend fun pauseDownload(downloadId: Long) {
        val torrent = activeTorrents[downloadId] ?: return
        torrent.pause()
    }

    override suspend fun cancelDownload(downloadId: Long) {
        activeTorrents.remove(downloadId)
        progressFlows.remove(downloadId)
        torrentProgressFlows.remove(downloadId)
        torrentStream.stopStream()
    }

    override suspend fun deleteDownload(downloadId: Long) {
        val torrent = activeTorrents[downloadId] ?: return
        activeTorrents.remove(downloadId)
        progressFlows.remove(downloadId)
        torrentProgressFlows.remove(downloadId)
        // Delete torrent files
        torrent.videoFile?.delete()
        torrentStream.stopStream()
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Download> {
        return progressFlows.getOrPut(downloadId) {
            MutableStateFlow(null)
        }.filterNotNull()
    }

    override fun isDownloadActive(downloadId: Long): Boolean {
        return activeTorrents.containsKey(downloadId)
    }

    private fun startProgressMonitoring(downloadId: Long, torrent: Torrent) {
        coroutineScope.launch {
            while (isActive) {
                try {
                    // Get current status (would need to query from torrent)
                    // This is a simplified version - actual implementation would
                    // need to properly track TorrentStream's status callbacks

                    delay(1000) // Update every second
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun extractInfoHashFromMagnet(magnetUri: String): String {
        val infoHashMatch = Regex("xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})")
            .find(magnetUri)
        return infoHashMatch?.groupValues?.get(1) ?: ""
    }

    fun shutdown() {
        coroutineScope.cancel()
        activeTorrents.clear()
        progressFlows.clear()
        torrentProgressFlows.clear()
        torrentStream.stopStream()
    }
}

// Dummy implementation for expect/actual requirement
private fun createDummyStream(): TorrentStream {
    throw IllegalStateException(
        "TorrentDownloadManager on Android requires Context. " +
        "Use TorrentDownloadManager.create(context, downloadDir) instead."
    )
}

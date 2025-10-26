package com.romreviewertools.downitup.data.torrent

import com.romreviewertools.downitup.domain.model.Download
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.libtorrent4j.*
import org.libtorrent4j.alerts.*
import java.io.File

/**
 * Desktop (JVM) implementation of TorrentDownloadManager using libtorrent4j
 */
actual class TorrentDownloadManager actual constructor() :
    com.romreviewertools.downitup.domain.manager.DownloadManager {

    private val sessionManager: SessionManager
    private val activeTorrents = mutableMapOf<Long, TorrentHandle>()
    private val torrentIdToDownloadId = mutableMapOf<Sha1Hash, Long>()
    private val progressFlows = mutableMapOf<Long, MutableStateFlow<Download?>>()
    private val torrentProgressFlows = mutableMapOf<Long, MutableStateFlow<TorrentProgress?>>()
    private val monitoringJobs = mutableMapOf<Long, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Create SessionManager with alert listener
        sessionManager = object : SessionManager() {
            override fun onBeforeStart() {
                super.onBeforeStart()
                // Enable DHT for magnet links
                val sp = SessionParams(SettingsPack())
                sp.settings().enableDht(true)
            }

            override fun onAfterStart() {
                super.onAfterStart()
                println("TorrentDownloadManager: SessionManager started")
            }
        }

        // Start session
        sessionManager.start()

        println("TorrentDownloadManager (JVM): Initialized with libtorrent4j ${LibTorrent.version()}")
    }

    actual suspend fun addMagnetLink(
        downloadId: Long,
        magnetUri: String,
        savePath: String
    ): String = withContext(Dispatchers.IO) {
        try {
            println("TorrentDownloadManager: Adding magnet link for download $downloadId")

            // Get save directory (remove filename if included)
            val saveDir = File(savePath).parentFile ?: File(savePath)

            // Download magnet and get handle
            val handle = sessionManager.download(magnetUri, saveDir)

            if (handle != null && handle.isValid) {
                val infoHash = handle.infoHash()
                activeTorrents[downloadId] = handle
                torrentIdToDownloadId[infoHash] = downloadId

                // Start progress monitoring
                startProgressMonitoring(downloadId, handle)

                println("TorrentDownloadManager: Magnet added successfully, info hash: ${infoHash.toHex()}")
                return@withContext infoHash.toHex()
            } else {
                throw Exception("Failed to add magnet link - invalid handle")
            }
        } catch (e: Exception) {
            println("TorrentDownloadManager: Error adding magnet: ${e.message}")
            throw e
        }
    }

    actual suspend fun addTorrentFile(
        downloadId: Long,
        torrentFilePath: String,
        savePath: String
    ): String = withContext(Dispatchers.IO) {
        try {
            println("TorrentDownloadManager: Adding torrent file for download $downloadId")

            val torrentFile = File(torrentFilePath)
            val saveDir = File(savePath).parentFile ?: File(savePath)

            // Download torrent file and get handle
            val handle = sessionManager.download(torrentFile, saveDir)

            if (handle != null && handle.isValid) {
                val infoHash = handle.infoHash()
                activeTorrents[downloadId] = handle
                torrentIdToDownloadId[infoHash] = downloadId

                // Start progress monitoring
                startProgressMonitoring(downloadId, handle)

                println("TorrentDownloadManager: Torrent file added successfully, info hash: ${infoHash.toHex()}")
                return@withContext infoHash.toHex()
            } else {
                throw Exception("Failed to add torrent file - invalid handle")
            }
        } catch (e: Exception) {
            println("TorrentDownloadManager: Error adding torrent file: ${e.message}")
            throw e
        }
    }

    actual suspend fun setSequentialDownload(downloadId: Long, enabled: Boolean) {
        activeTorrents[downloadId]?.setSequentialDownload(enabled)
        println("TorrentDownloadManager: Sequential download set to $enabled for download $downloadId")
    }

    actual fun getTorrentProgress(downloadId: Long): Flow<TorrentProgress> {
        return torrentProgressFlows.getOrPut(downloadId) {
            MutableStateFlow(null)
        }.filterNotNull()
    }

    actual fun parseMagnetLink(magnetUri: String): TorrentMetadata {
        val infoHashMatch = Regex("xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})")
            .find(magnetUri)
        val infoHash = infoHashMatch?.groupValues?.get(1) ?: ""

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
        val handle = activeTorrents[downloadId]
        if (handle != null && handle.isValid) {
            handle.resume()
            println("TorrentDownloadManager: Started/resumed download $downloadId")
        } else {
            println("TorrentDownloadManager: Cannot start - handle not found or invalid for download $downloadId")
        }
    }

    override suspend fun pauseDownload(downloadId: Long) {
        val handle = activeTorrents[downloadId]
        if (handle != null && handle.isValid) {
            handle.pause()
            println("TorrentDownloadManager: Paused download $downloadId")
        }
    }

    override suspend fun cancelDownload(downloadId: Long) {
        val handle = activeTorrents[downloadId]
        if (handle != null && handle.isValid) {
            // Stop monitoring
            monitoringJobs[downloadId]?.cancel()
            monitoringJobs.remove(downloadId)

            // Remove from session
            sessionManager.remove(handle)

            val infoHash = handle.infoHash()
            activeTorrents.remove(downloadId)
            torrentIdToDownloadId.remove(infoHash)
            progressFlows.remove(downloadId)
            torrentProgressFlows.remove(downloadId)

            println("TorrentDownloadManager: Cancelled download $downloadId")
        }
    }

    override suspend fun deleteDownload(downloadId: Long) {
        val handle = activeTorrents[downloadId]
        if (handle != null && handle.isValid) {
            // Stop monitoring
            monitoringJobs[downloadId]?.cancel()
            monitoringJobs.remove(downloadId)

            // Remove torrent and delete files
            sessionManager.remove(handle, SessionHandle.DELETE_FILES)

            val infoHash = handle.infoHash()
            activeTorrents.remove(downloadId)
            torrentIdToDownloadId.remove(infoHash)
            progressFlows.remove(downloadId)
            torrentProgressFlows.remove(downloadId)

            println("TorrentDownloadManager: Deleted download $downloadId with files")
        }
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Download> {
        return progressFlows.getOrPut(downloadId) {
            MutableStateFlow(null)
        }.filterNotNull()
    }

    override fun isDownloadActive(downloadId: Long): Boolean {
        return activeTorrents.containsKey(downloadId)
    }

    private fun startProgressMonitoring(downloadId: Long, handle: TorrentHandle) {
        val job = coroutineScope.launch {
            while (isActive && handle.isValid) {
                try {
                    val status = handle.status()

                    // Update torrent-specific progress
                    val torrentProgress = TorrentProgress(
                        downloadId = downloadId,
                        progress = status.progress(),
                        downloadSpeed = status.downloadRate().toLong(),
                        uploadSpeed = status.uploadRate().toLong(),
                        downloadedBytes = status.totalDone(),
                        totalBytes = status.totalWanted(),
                        numSeeds = status.numSeeds(),
                        numPeers = status.numPeers(),
                        state = mapTorrentState(status.state())
                    )
                    torrentProgressFlows[downloadId]?.emit(torrentProgress)

                    // TODO: Update database with progress

                    delay(1000) // Update every second
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        println("TorrentDownloadManager: Error monitoring progress: ${e.message}")
                    }
                    break
                }
            }
        }
        monitoringJobs[downloadId] = job
    }

    private fun mapTorrentState(state: TorrentStatus.State): TorrentState {
        return when (state) {
            TorrentStatus.State.CHECKING_FILES -> TorrentState.CHECKING_FILES
            TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DOWNLOADING_METADATA
            TorrentStatus.State.DOWNLOADING -> TorrentState.DOWNLOADING
            TorrentStatus.State.FINISHED -> TorrentState.FINISHED
            TorrentStatus.State.SEEDING -> TorrentState.SEEDING
            TorrentStatus.State.ALLOCATING -> TorrentState.ALLOCATING
            TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CHECKING_RESUME_DATA
            else -> TorrentState.DOWNLOADING
        }
    }

    fun shutdown() {
        println("TorrentDownloadManager: Shutting down...")
        coroutineScope.cancel()
        monitoringJobs.values.forEach { it.cancel() }
        activeTorrents.clear()
        progressFlows.clear()
        torrentProgressFlows.clear()
        sessionManager.stop()
        println("TorrentDownloadManager: Shutdown complete")
    }
}

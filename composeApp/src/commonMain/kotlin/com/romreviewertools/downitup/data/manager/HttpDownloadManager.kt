package com.romreviewertools.downitup.data.manager

import com.romreviewertools.downitup.data.file.FileWriter
import com.romreviewertools.downitup.data.file.createFileWriter
import com.romreviewertools.downitup.data.local.AppDatabase
import com.romreviewertools.downitup.data.local.model.DownloadStatus
import com.romreviewertools.downitup.domain.manager.DownloadManager
import com.romreviewertools.downitup.domain.model.Download
import com.romreviewertools.downitup.domain.model.toDomain
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * HTTP Download Manager using Ktor Client
 * Supports resumable downloads with progress tracking and file writing
 * Supports multi-connection (chunked) downloads for faster speeds
 */
class HttpDownloadManager(
    private val httpClient: HttpClient,
    private val database: AppDatabase,
    private val fileWriter: FileWriter = createFileWriter(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : DownloadManager {

    companion object {
        const val DEFAULT_CONNECTION_COUNT = 4
        const val MAX_CONNECTION_COUNT = 16
        const val MIN_CONNECTION_COUNT = 1
        const val MIN_CHUNK_SIZE = 1_048_576L // 1 MB minimum per chunk
        const val MIN_FILE_SIZE_FOR_CHUNKING = 10_485_760L // 10 MB minimum for multi-connection
    }

    private val downloadQueries = database.downloadQueries
    private val chunkQueries = database.downloadChunkQueries
    private val activeDownloads = mutableMapOf<Long, Job>()
    private val activeChunks = mutableMapOf<Long, MutableList<Job>>() // downloadId -> list of chunk jobs
    private val progressFlows = mutableMapOf<Long, MutableStateFlow<Download?>>()

    override suspend fun startDownload(downloadId: Long) {
        println("HttpDownloadManager.startDownload: Starting download ID: $downloadId")

        // Check if already downloading
        if (activeDownloads.containsKey(downloadId)) {
            println("HttpDownloadManager.startDownload: Download $downloadId already active")
            return
        }

        val downloadEntity = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
        if (downloadEntity == null) {
            println("HttpDownloadManager.startDownload: ERROR - Download $downloadId not found in database")
            return
        }

        println("HttpDownloadManager.startDownload: Found download - URL: ${downloadEntity.url}, Path: ${downloadEntity.savePath}")
        println("HttpDownloadManager.startDownload: Connection count: ${downloadEntity.connectionCount}, Chunked: ${downloadEntity.chunkedDownload}")

        // Update status to DOWNLOADING
        downloadQueries.updateStatus(DownloadStatus.DOWNLOADING.name, downloadId)
        println("HttpDownloadManager.startDownload: Status updated to DOWNLOADING")

        // Start download job
        val job = coroutineScope.launch {
            try {
                println("HttpDownloadManager.startDownload: Launching download coroutine for ID: $downloadId")

                // Decide whether to use multi-connection or single connection
                if (downloadEntity.chunkedDownload != 0L && downloadEntity.connectionCount > 1) {
                    println("HttpDownloadManager.startDownload: Using multi-connection download")
                    downloadFileMultiConnection(downloadId, downloadEntity.url, downloadEntity.savePath, downloadEntity.connectionCount.toInt())
                } else {
                    println("HttpDownloadManager.startDownload: Using single-connection download")
                    downloadFileSingleConnection(downloadId, downloadEntity.url, downloadEntity.savePath)
                }

                println("HttpDownloadManager.startDownload: Download completed successfully for ID: $downloadId")
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Download was paused/cancelled
                println("HttpDownloadManager.startDownload: Download $downloadId cancelled")
                downloadQueries.updateStatus(DownloadStatus.PAUSED.name, downloadId)
            } catch (e: Exception) {
                // Download failed
                val errorMsg = "${e::class.simpleName}: ${e.message}"
                println("HttpDownloadManager.startDownload: ERROR - Download $downloadId failed: $errorMsg")
                println("HttpDownloadManager.startDownload: Full error details:")
                e.printStackTrace()
                downloadQueries.updateDownloadError(
                    error = errorMsg,
                    status = DownloadStatus.FAILED.name,
                    id = downloadId
                )
            } finally {
                activeDownloads.remove(downloadId)
                activeChunks.remove(downloadId)
                println("HttpDownloadManager.startDownload: Removed download $downloadId from active downloads")
            }
        }

        activeDownloads[downloadId] = job
        println("HttpDownloadManager.startDownload: Added download $downloadId to active downloads")
    }

    /**
     * Check if server supports HTTP Range requests
     */
    private suspend fun checkRangeSupport(url: String): Boolean {
        return try {
            httpClient.head(url).headers[HttpHeaders.AcceptRanges] != null
        } catch (e: Exception) {
            println("HttpDownloadManager.checkRangeSupport: Failed to check range support: ${e.message}")
            false
        }
    }

    /**
     * Multi-connection download implementation
     */
    private suspend fun downloadFileMultiConnection(
        downloadId: Long,
        url: String,
        savePath: String,
        connectionCount: Int
    ) {
        println("HttpDownloadManager.downloadFileMultiConnection: === STARTING MULTI-CONNECTION DOWNLOAD ===")
        println("HttpDownloadManager.downloadFileMultiConnection: ID: $downloadId, Connections: $connectionCount")

        val downloadEntity = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
        if (downloadEntity == null) {
            println("HttpDownloadManager.downloadFileMultiConnection: ERROR - Download entity not found")
            return
        }

        var totalBytes = downloadEntity.totalBytes

        // If total bytes not known, get it from HEAD request
        if (totalBytes == 0L) {
            try {
                val headResponse = httpClient.head(url)
                totalBytes = headResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
                if (totalBytes > 0) {
                    downloadQueries.updateTotalBytes(totalBytes, downloadId)
                }
                println("HttpDownloadManager.downloadFileMultiConnection: Total bytes from HEAD: $totalBytes")
            } catch (e: Exception) {
                println("HttpDownloadManager.downloadFileMultiConnection: Failed to get content length: ${e.message}")
                // Fall back to single connection
                return downloadFileSingleConnection(downloadId, url, savePath)
            }
        }

        // Check if server supports range requests
        if (!checkRangeSupport(url)) {
            println("HttpDownloadManager.downloadFileMultiConnection: Server doesn't support Range requests, falling back to single connection")
            return downloadFileSingleConnection(downloadId, url, savePath)
        }

        // Check if file is large enough for chunking
        if (totalBytes < MIN_FILE_SIZE_FOR_CHUNKING) {
            println("HttpDownloadManager.downloadFileMultiConnection: File too small for chunking, falling back to single connection")
            return downloadFileSingleConnection(downloadId, url, savePath)
        }

        // Check if chunks already exist (resuming)
        val existingChunks = chunkQueries.getChunksByDownloadId(downloadId).executeAsList()

        val chunks = if (existingChunks.isEmpty()) {
            // Create new chunks
            println("HttpDownloadManager.downloadFileMultiConnection: Creating ${connectionCount} chunks")
            createAndSaveChunks(downloadId, totalBytes, connectionCount)
        } else {
            println("HttpDownloadManager.downloadFileMultiConnection: Resuming with ${existingChunks.size} existing chunks")
            existingChunks
        }

        // Download chunks in parallel
        activeChunks[downloadId] = mutableListOf()

        coroutineScope {
            chunks.map { chunkEntity ->
                val job = async {
                    downloadChunk(downloadId, chunkEntity.id, chunkEntity.chunkIndex.toInt(),
                                 chunkEntity.startByte, chunkEntity.endByte,
                                 chunkEntity.downloadedBytes, url, savePath)
                }
                activeChunks[downloadId]?.add(job)
                job
            }.awaitAll()
        }

        // All chunks completed - mark download as complete
        downloadQueries.updateDownloadCompleted(
            status = DownloadStatus.COMPLETED.name,
            downloadedBytes = totalBytes,
            downloadSpeed = 0L,
            completedAt = System.currentTimeMillis(),
            id = downloadId
        )

        println("HttpDownloadManager.downloadFileMultiConnection: Multi-connection download completed")
    }

    /**
     * Create and save chunks to database
     */
    private fun createAndSaveChunks(
        downloadId: Long,
        totalBytes: Long,
        connectionCount: Int
    ): List<com.romreviewertools.downitup.data.local.Download_chunks> {
        val chunkSize = totalBytes / connectionCount
        val chunks = mutableListOf<com.romreviewertools.downitup.data.local.Download_chunks>()

        for (i in 0 until connectionCount) {
            val startByte = i * chunkSize
            val endByte = if (i == connectionCount - 1) {
                totalBytes - 1 // Last chunk gets remaining bytes
            } else {
                (i + 1) * chunkSize - 1
            }

            chunkQueries.insertChunk(
                downloadId = downloadId,
                chunkIndex = i.toLong(),
                startByte = startByte,
                endByte = endByte,
                downloadedBytes = 0L,
                status = com.romreviewertools.downitup.data.local.model.ChunkStatus.QUEUED.name,
                speed = 0L
            )

            println("HttpDownloadManager.createAndSaveChunks: Created chunk $i: $startByte-$endByte")
        }

        return chunkQueries.getChunksByDownloadId(downloadId).executeAsList()
    }

    /**
     * Download a single chunk
     */
    private suspend fun downloadChunk(
        downloadId: Long,
        chunkId: Long,
        chunkIndex: Int,
        startByte: Long,
        endByte: Long,
        alreadyDownloaded: Long,
        url: String,
        savePath: String
    ) {
        println("HttpDownloadManager.downloadChunk: Starting chunk $chunkIndex ($startByte-$endByte)")

        // Update chunk status to DOWNLOADING
        chunkQueries.updateChunkStatus(
            status = com.romreviewertools.downitup.data.local.model.ChunkStatus.DOWNLOADING.name,
            id = chunkId
        )

        // Create file handle and seek to position
        val fileHandle = fileWriter.createWriter(savePath)

        try {
            val resumeFrom = startByte + alreadyDownloaded
            fileHandle.seek(resumeFrom)

            println("HttpDownloadManager.downloadChunk: Chunk $chunkIndex resuming from byte $resumeFrom")

            httpClient.prepareGet(url) {
                header(HttpHeaders.Range, "bytes=$resumeFrom-$endByte")
            }.execute { response ->
                println("HttpDownloadManager.downloadChunk: Chunk $chunkIndex - Response: ${response.status}")

                val channel: ByteReadChannel = response.bodyAsChannel()
                var downloadedBytes = alreadyDownloaded
                var lastUpdateTime = System.currentTimeMillis()
                var lastDownloadedBytes = alreadyDownloaded
                val buffer = ByteArray(8192) // 8KB buffer

                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead == -1) break

                    // Write bytes to file
                    fileHandle.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastUpdateTime

                    // Update progress every 500ms
                    if (timeDiff >= 500) {
                        val bytesDiff = downloadedBytes - lastDownloadedBytes
                        val speed = if (timeDiff > 0) {
                            (bytesDiff * 1000L) / timeDiff
                        } else {
                            0L
                        }

                        // Update chunk progress in database
                        chunkQueries.updateChunkProgress(
                            downloadedBytes = downloadedBytes,
                            speed = speed,
                            id = chunkId
                        )

                        // Update overall download progress
                        updateOverallProgress(downloadId)

                        lastUpdateTime = currentTime
                        lastDownloadedBytes = downloadedBytes
                    }
                }

                // Chunk complete
                chunkQueries.updateChunkStatus(
                    status = com.romreviewertools.downitup.data.local.model.ChunkStatus.COMPLETED.name,
                    id = chunkId
                )

                println("HttpDownloadManager.downloadChunk: Chunk $chunkIndex completed")
            }
        } catch (e: Exception) {
            println("HttpDownloadManager.downloadChunk: Chunk $chunkIndex failed: ${e.message}")
            chunkQueries.updateChunkStatus(
                status = com.romreviewertools.downitup.data.local.model.ChunkStatus.FAILED.name,
                id = chunkId
            )
            throw e
        } finally {
            fileHandle.close()
        }
    }

    /**
     * Update overall download progress by summing all chunks
     */
    private fun updateOverallProgress(downloadId: Long) {
        val chunks = chunkQueries.getChunksByDownloadId(downloadId).executeAsList()
        val totalDownloaded = chunks.sumOf { it.downloadedBytes }
        val totalSpeed = chunks.sumOf { it.speed }

        downloadQueries.updateDownloadProgress(
            downloadedBytes = totalDownloaded,
            downloadSpeed = totalSpeed,
            id = downloadId
        )

        // Update flow for UI with chunks included
        val chunkDomainList = chunks.map { chunkEntity -> chunkEntity.toDomain() }
        val updatedDownload = downloadQueries.getDownloadById(downloadId)
            .executeAsOneOrNull()?.toDomain(chunkDomainList)
        updatedDownload?.let { download ->
            progressFlows[downloadId]?.value = download
        }
    }

    /**
     * Single-connection download (original implementation)
     */
    private suspend fun downloadFileSingleConnection(downloadId: Long, url: String, savePath: String) {
        println("HttpDownloadManager.downloadFileSingleConnection: === STARTING SINGLE-CONNECTION DOWNLOAD ===")
        println("HttpDownloadManager.downloadFileSingleConnection: ID: $downloadId")
        println("HttpDownloadManager.downloadFileSingleConnection: URL: $url")
        println("HttpDownloadManager.downloadFileSingleConnection: SavePath: $savePath")

        val downloadEntity = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
        if (downloadEntity == null) {
            println("HttpDownloadManager.downloadFileSingleConnection: ERROR - Download entity not found")
            return
        }

        val startByte = downloadEntity.downloadedBytes
        println("HttpDownloadManager.downloadFileSingleConnection: Start byte: $startByte")

        // Create file handle
        println("HttpDownloadManager.downloadFileSingleConnection: Creating file handle...")
        val fileHandle = try {
            val handle = fileWriter.createWriter(savePath)
            println("HttpDownloadManager.downloadFileSingleConnection: File handle created successfully")
            handle
        } catch (e: Exception) {
            val errorMsg = "Failed to create file: ${e::class.simpleName} - ${e.message}"
            println("HttpDownloadManager.downloadFileSingleConnection: ERROR - $errorMsg")
            println("HttpDownloadManager.downloadFileSingleConnection: Stack trace:")
            e.printStackTrace()
            downloadQueries.updateDownloadError(
                error = errorMsg,
                status = DownloadStatus.FAILED.name,
                id = downloadId
            )
            return
        }

        try {
            // Seek to resume position if resuming
            if (startByte > 0) {
                println("HttpDownloadManager.downloadFileSingleConnection: Seeking to position $startByte")
                fileHandle.seek(startByte)
            }

            println("HttpDownloadManager.downloadFileSingleConnection: === PREPARING HTTP REQUEST ===")
            println("HttpDownloadManager.downloadFileSingleConnection: URL: $url")
            println("HttpDownloadManager.downloadFileSingleConnection: HttpClient: ${httpClient::class.simpleName}")

            try {
                httpClient.prepareGet(url) {
                    println("HttpDownloadManager.downloadFileSingleConnection: Inside prepareGet block")
                    if (startByte > 0) {
                        // Resume download from last position
                        header(HttpHeaders.Range, "bytes=$startByte-")
                        println("HttpDownloadManager.downloadFileSingleConnection: Added Range header: bytes=$startByte-")
                    }
                }.execute { response ->
                    println("HttpDownloadManager.downloadFileSingleConnection: === HTTP RESPONSE RECEIVED ===")
                    println("HttpDownloadManager.downloadFileSingleConnection: Status: ${response.status}")
                    println("HttpDownloadManager.downloadFileSingleConnection: Headers: ${response.headers}")
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
                val totalBytes = if (startByte > 0) {
                    // For resumed downloads, add the already downloaded bytes
                    startByte + contentLength
                } else {
                    contentLength
                }

                // Update total bytes if not set
                if (downloadEntity.totalBytes == 0L && totalBytes > 0) {
                    downloadQueries.updateTotalBytes(totalBytes, downloadId)
                }

                val channel: ByteReadChannel = response.bodyAsChannel()
                var downloadedBytes = startByte
                var lastUpdateTime = System.currentTimeMillis()
                var lastDownloadedBytes = startByte
                val buffer = ByteArray(8192) // 8KB buffer

                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead == -1) break

                    // Write bytes to file
                    fileHandle.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastUpdateTime

                    // Update progress every 500ms
                    if (timeDiff >= 500) {
                        val bytesDiff = downloadedBytes - lastDownloadedBytes
                        val speed = if (timeDiff > 0) {
                            (bytesDiff * 1000L) / timeDiff
                        } else {
                            0L
                        }

                        // Update database
                        downloadQueries.updateDownloadProgress(
                            downloadedBytes = downloadedBytes,
                            downloadSpeed = speed,
                            id = downloadId
                        )

                        // Update flow for UI
                        val updatedDownload = downloadQueries.getDownloadById(downloadId)
                            .executeAsOneOrNull()?.toDomain()
                        updatedDownload?.let {
                            progressFlows[downloadId]?.value = it
                        }

                        lastUpdateTime = currentTime
                        lastDownloadedBytes = downloadedBytes
                    }
                }

                // Download complete
                downloadQueries.updateDownloadCompleted(
                    status = DownloadStatus.COMPLETED.name,
                    downloadedBytes = totalBytes,
                    downloadSpeed = 0L,
                    completedAt = System.currentTimeMillis(),
                    id = downloadId
                )

                // Final update to flow
                val completedDownload = downloadQueries.getDownloadById(downloadId)
                    .executeAsOneOrNull()?.toDomain()
                completedDownload?.let {
                    progressFlows[downloadId]?.value = it
                }
            }
            } catch (e: Exception) {
                println("HttpDownloadManager.downloadFileSingleConnection: === HTTP REQUEST FAILED ===")
                println("HttpDownloadManager.downloadFileSingleConnection: Exception type: ${e::class.simpleName}")
                println("HttpDownloadManager.downloadFileSingleConnection: Exception message: ${e.message}")
                println("HttpDownloadManager.downloadFileSingleConnection: Full stack trace:")
                e.printStackTrace()

                // Check if it's an SSL error
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("chain validation failed", ignoreCase = true) ||
                    errorMessage.contains("SSL", ignoreCase = true) ||
                    errorMessage.contains("TLS", ignoreCase = true) ||
                    errorMessage.contains("certificate", ignoreCase = true)) {
                    println("HttpDownloadManager.downloadFileSingleConnection: *** SSL/TLS ERROR DETECTED ***")
                    println("HttpDownloadManager.downloadFileSingleConnection: This is an SSL certificate validation error")
                }

                throw e
            }
        } catch (e: Exception) {
            println("HttpDownloadManager.downloadFileSingleConnection: === DOWNLOAD FAILED (OUTER CATCH) ===")
            println("HttpDownloadManager.downloadFileSingleConnection: Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            // Always close file handle
            println("HttpDownloadManager.downloadFileSingleConnection: Closing file handle")
            fileHandle.close()
            println("HttpDownloadManager.downloadFileSingleConnection: File handle closed")
        }
    }

    override suspend fun pauseDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)

        // Cancel all chunk jobs
        activeChunks[downloadId]?.forEach { it.cancel() }
        activeChunks.remove(downloadId)

        // Update chunk statuses to PAUSED
        val chunks = chunkQueries.getChunksByDownloadId(downloadId).executeAsList()
        val downloadingChunks = chunks.filter { chunkEntity ->
            chunkEntity.status == com.romreviewertools.downitup.data.local.model.ChunkStatus.DOWNLOADING.name
        }
        downloadingChunks.forEach { chunkEntity ->
            chunkQueries.updateChunkStatus(
                status = com.romreviewertools.downitup.data.local.model.ChunkStatus.PAUSED.name,
                id = chunkEntity.id
            )
        }

        downloadQueries.updateStatus(DownloadStatus.PAUSED.name, downloadId)
    }

    override suspend fun cancelDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)

        // Cancel all chunk jobs
        activeChunks[downloadId]?.forEach { it.cancel() }
        activeChunks.remove(downloadId)

        // Delete chunks from database
        chunkQueries.deleteChunksByDownloadId(downloadId)

        // Delete partial file
        val download = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
        download?.savePath?.let { path ->
            try {
                val fileHandle = fileWriter.createWriter(path)
                fileHandle.delete()
            } catch (e: Exception) {
                // Ignore deletion errors
            }
        }

        downloadQueries.updateStatus(DownloadStatus.CANCELLED.name, downloadId)
    }

    override suspend fun deleteDownload(downloadId: Long) {
        // Cancel if active
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)

        // Cancel all chunk jobs
        activeChunks[downloadId]?.forEach { it.cancel() }
        activeChunks.remove(downloadId)

        progressFlows.remove(downloadId)

        // Delete chunks from database
        chunkQueries.deleteChunksByDownloadId(downloadId)

        // Delete file from disk
        val download = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
        download?.savePath?.let { path ->
            try {
                val fileHandle = fileWriter.createWriter(path)
                fileHandle.delete()
            } catch (e: Exception) {
                // Ignore deletion errors
            }
        }

        // Delete from database
        downloadQueries.deleteDownload(downloadId)
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Download> {
        return progressFlows.getOrPut(downloadId) {
            MutableStateFlow<Download?>(null).also { flow ->
                // Initialize with current state
                coroutineScope.launch {
                    val downloadEntity = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
                    val chunkEntities = chunkQueries.getChunksByDownloadId(downloadId).executeAsList()
                    val chunkDomainList = chunkEntities.map { chunkEntity -> chunkEntity.toDomain() }
                    val download = downloadEntity?.toDomain(chunkDomainList)
                    flow.value = download
                }
            }
        }.filterNotNull()
    }

    override fun isDownloadActive(downloadId: Long): Boolean {
        return activeDownloads.containsKey(downloadId) &&
               activeDownloads[downloadId]?.isActive == true
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        activeChunks.values.forEach { chunkJobs ->
            chunkJobs.forEach { it.cancel() }
        }
        activeChunks.clear()
        progressFlows.clear()
    }
}

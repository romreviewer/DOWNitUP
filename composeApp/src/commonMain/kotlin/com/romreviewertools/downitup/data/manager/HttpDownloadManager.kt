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
 */
class HttpDownloadManager(
    private val httpClient: HttpClient,
    private val database: AppDatabase,
    private val fileWriter: FileWriter = createFileWriter(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : DownloadManager {

    private val downloadQueries = database.downloadQueries
    private val activeDownloads = mutableMapOf<Long, Job>()
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

        // Update status to DOWNLOADING
        downloadQueries.updateStatus(DownloadStatus.DOWNLOADING.name, downloadId)
        println("HttpDownloadManager.startDownload: Status updated to DOWNLOADING")

        // Start download job
        val job = coroutineScope.launch {
            try {
                println("HttpDownloadManager.startDownload: Launching download coroutine for ID: $downloadId")
                downloadFile(downloadId, downloadEntity.url, downloadEntity.savePath)
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
                println("HttpDownloadManager.startDownload: Removed download $downloadId from active downloads")
            }
        }

        activeDownloads[downloadId] = job
        println("HttpDownloadManager.startDownload: Added download $downloadId to active downloads")
    }

    private suspend fun downloadFile(downloadId: Long, url: String, savePath: String) {
        println("HttpDownloadManager.downloadFile: === STARTING DOWNLOAD FILE ===")
        println("HttpDownloadManager.downloadFile: ID: $downloadId")
        println("HttpDownloadManager.downloadFile: URL: $url")
        println("HttpDownloadManager.downloadFile: SavePath: $savePath")

        val downloadEntity = downloadQueries.getDownloadById(downloadId).executeAsOneOrNull()
        if (downloadEntity == null) {
            println("HttpDownloadManager.downloadFile: ERROR - Download entity not found")
            return
        }

        val startByte = downloadEntity.downloadedBytes
        println("HttpDownloadManager.downloadFile: Start byte: $startByte")

        // Create file handle
        println("HttpDownloadManager.downloadFile: Creating file handle...")
        val fileHandle = try {
            val handle = fileWriter.createWriter(savePath)
            println("HttpDownloadManager.downloadFile: File handle created successfully")
            handle
        } catch (e: Exception) {
            val errorMsg = "Failed to create file: ${e::class.simpleName} - ${e.message}"
            println("HttpDownloadManager.downloadFile: ERROR - $errorMsg")
            println("HttpDownloadManager.downloadFile: Stack trace:")
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
                println("HttpDownloadManager.downloadFile: Seeking to position $startByte")
                fileHandle.seek(startByte)
            }

            println("HttpDownloadManager.downloadFile: === PREPARING HTTP REQUEST ===")
            println("HttpDownloadManager.downloadFile: URL: $url")
            println("HttpDownloadManager.downloadFile: HttpClient: ${httpClient::class.simpleName}")

            try {
                httpClient.prepareGet(url) {
                    println("HttpDownloadManager.downloadFile: Inside prepareGet block")
                    if (startByte > 0) {
                        // Resume download from last position
                        header(HttpHeaders.Range, "bytes=$startByte-")
                        println("HttpDownloadManager.downloadFile: Added Range header: bytes=$startByte-")
                    }
                }.execute { response ->
                    println("HttpDownloadManager.downloadFile: === HTTP RESPONSE RECEIVED ===")
                    println("HttpDownloadManager.downloadFile: Status: ${response.status}")
                    println("HttpDownloadManager.downloadFile: Headers: ${response.headers}")
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
                println("HttpDownloadManager.downloadFile: === HTTP REQUEST FAILED ===")
                println("HttpDownloadManager.downloadFile: Exception type: ${e::class.simpleName}")
                println("HttpDownloadManager.downloadFile: Exception message: ${e.message}")
                println("HttpDownloadManager.downloadFile: Full stack trace:")
                e.printStackTrace()

                // Check if it's an SSL error
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("chain validation failed", ignoreCase = true) ||
                    errorMessage.contains("SSL", ignoreCase = true) ||
                    errorMessage.contains("TLS", ignoreCase = true) ||
                    errorMessage.contains("certificate", ignoreCase = true)) {
                    println("HttpDownloadManager.downloadFile: *** SSL/TLS ERROR DETECTED ***")
                    println("HttpDownloadManager.downloadFile: This is an SSL certificate validation error")
                }

                throw e
            }
        } catch (e: Exception) {
            println("HttpDownloadManager.downloadFile: === DOWNLOAD FAILED (OUTER CATCH) ===")
            println("HttpDownloadManager.downloadFile: Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            // Always close file handle
            println("HttpDownloadManager.downloadFile: Closing file handle")
            fileHandle.close()
            println("HttpDownloadManager.downloadFile: File handle closed")
        }
    }

    override suspend fun pauseDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
        downloadQueries.updateStatus(DownloadStatus.PAUSED.name, downloadId)
    }

    override suspend fun cancelDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)

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
        progressFlows.remove(downloadId)

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
                    val download = downloadQueries.getDownloadById(downloadId)
                        .executeAsOneOrNull()?.toDomain()
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
        progressFlows.clear()
    }
}

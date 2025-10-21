package com.romreviewertools.downitup.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romreviewertools.downitup.data.local.AppDatabase
import com.romreviewertools.downitup.data.local.model.DownloadStatus
import com.romreviewertools.downitup.data.local.model.DownloadType
import com.romreviewertools.downitup.domain.model.Download
import com.romreviewertools.downitup.domain.model.DownloadChunk
import com.romreviewertools.downitup.domain.model.toDomain
import com.romreviewertools.downitup.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DownloadRepositoryImpl(
    private val database: AppDatabase
) : DownloadRepository {

    private val queries = database.downloadQueries
    private val chunkQueries = database.downloadChunkQueries

    override fun getAllDownloads(): Flow<List<Download>> {
        return queries.getAllDownloads()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { downloadEntity ->
                    val chunks = chunkQueries.getChunksByDownloadId(downloadEntity.id)
                        .executeAsList()
                        .map { it.toDomain() }
                    downloadEntity.toDomain(chunks)
                }
            }
    }

    override fun getActiveDownloads(): Flow<List<Download>> {
        val activeStatuses = listOf(
            DownloadStatus.QUEUED.name,
            DownloadStatus.DOWNLOADING.name,
            DownloadStatus.PAUSED.name
        )
        return queries.getDownloadsByStatuses(activeStatuses)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { downloadEntity ->
                    val chunks = chunkQueries.getChunksByDownloadId(downloadEntity.id)
                        .executeAsList()
                        .map { it.toDomain() }
                    downloadEntity.toDomain(chunks)
                }
            }
    }

    override fun getCompletedDownloads(): Flow<List<Download>> {
        return queries.getDownloadsByStatus(DownloadStatus.COMPLETED.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { downloadEntity ->
                    val chunks = chunkQueries.getChunksByDownloadId(downloadEntity.id)
                        .executeAsList()
                        .map { it.toDomain() }
                    downloadEntity.toDomain(chunks)
                }
            }
    }

    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>> {
        return queries.getDownloadsByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { downloadEntity ->
                    val chunks = chunkQueries.getChunksByDownloadId(downloadEntity.id)
                        .executeAsList()
                        .map { it.toDomain() }
                    downloadEntity.toDomain(chunks)
                }
            }
    }

    override suspend fun getDownloadById(id: Long): Download? {
        return withContext(Dispatchers.Default) {
            val downloadEntity = queries.getDownloadById(id).executeAsOneOrNull()
            downloadEntity?.let {
                val chunks = chunkQueries.getChunksByDownloadId(it.id)
                    .executeAsList()
                    .map { chunk -> chunk.toDomain() }
                it.toDomain(chunks)
            }
        }
    }

    override suspend fun addHttpDownload(
        url: String,
        name: String,
        savePath: String,
        connectionCount: Int,
        useMultiConnection: Boolean
    ): Long {
        return withContext(Dispatchers.Default) {
            println("DownloadRepository: Inserting download - URL: $url, Name: $name, Connections: $connectionCount")

            queries.insertDownload(
                name = name,
                url = url,
                downloadType = DownloadType.HTTP.name,
                status = DownloadStatus.QUEUED.name,
                totalBytes = 0,
                downloadedBytes = 0,
                downloadSpeed = 0,
                savePath = savePath,
                mimeType = null,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                error = null,
                infoHash = null,
                seeders = 0,
                peers = 0,
                magnetUri = null,
                connectionCount = connectionCount.toLong(),
                chunkedDownload = if (useMultiConnection) 1 else 0
            )

            val insertedId = queries.lastInsertedRowId().executeAsOne()
            println("DownloadRepository: Insert complete, lastInsertedRowId: $insertedId")

            // Verify the download was actually inserted
            val allDownloads = queries.getAllDownloads().executeAsList()
            println("DownloadRepository: Total downloads in database: ${allDownloads.size}")
            allDownloads.forEach { download ->
                println("DownloadRepository: - Download ID: ${download.id}, Name: ${download.name}, Status: ${download.status}")
            }

            // Find the actual inserted download
            val actualInserted = allDownloads.maxByOrNull { it.id }
            if (actualInserted != null) {
                println("DownloadRepository: Using actual max ID: ${actualInserted.id}")
                actualInserted.id
            } else {
                println("DownloadRepository: WARNING - No downloads found, returning $insertedId")
                insertedId
            }
        }
    }

    override suspend fun addTorrentDownload(
        magnetUri: String,
        name: String,
        savePath: String,
        infoHash: String?
    ): Long {
        return withContext(Dispatchers.Default) {
            queries.insertDownload(
                name = name,
                url = magnetUri,
                downloadType = DownloadType.TORRENT.name,
                status = DownloadStatus.QUEUED.name,
                totalBytes = 0,
                downloadedBytes = 0,
                downloadSpeed = 0,
                savePath = savePath,
                mimeType = null,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                error = null,
                infoHash = infoHash,
                seeders = 0,
                peers = 0,
                magnetUri = magnetUri,
                connectionCount = 1,
                chunkedDownload = 0
            )
            queries.lastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun updateDownload(download: Download) {
        withContext(Dispatchers.Default) {
            queries.updateDownload(
                id = download.id,
                name = download.name,
                url = download.url,
                downloadType = download.downloadType.name,
                status = download.status.name,
                totalBytes = download.totalBytes,
                downloadedBytes = download.downloadedBytes,
                downloadSpeed = download.downloadSpeed,
                savePath = download.savePath,
                mimeType = download.mimeType,
                completedAt = download.completedAt,
                error = download.error,
                seeders = download.seeders.toLong(),
                peers = download.peers.toLong()
            )
        }
    }

    override suspend fun updateDownloadProgress(id: Long, downloadedBytes: Long, downloadSpeed: Long) {
        withContext(Dispatchers.Default) {
            queries.updateDownloadProgress(
                id = id,
                downloadedBytes = downloadedBytes,
                downloadSpeed = downloadSpeed
            )
        }
    }

    override suspend fun updateDownloadStatus(id: Long, status: DownloadStatus, error: String?) {
        withContext(Dispatchers.Default) {
            val completedAt = if (status == DownloadStatus.COMPLETED) {
                System.currentTimeMillis()
            } else null

            queries.updateDownloadStatus(
                id = id,
                status = status.name,
                error = error,
                completedAt = completedAt
            )
        }
    }

    override suspend fun deleteDownload(id: Long, deleteFiles: Boolean) {
        withContext(Dispatchers.Default) {
            // TODO: Implement file deletion if deleteFiles is true
            queries.deleteDownload(id)
        }
    }

    override suspend fun deleteAllDownloads() {
        withContext(Dispatchers.Default) {
            queries.deleteAllDownloads()
        }
    }

    override suspend fun getDownloadCount(): Int {
        return withContext(Dispatchers.Default) {
            queries.getDownloadCount().executeAsOne().toInt()
        }
    }

    override suspend fun getDownloadCountByStatus(status: DownloadStatus): Int {
        return withContext(Dispatchers.Default) {
            queries.getDownloadCountByStatus(status.name).executeAsOne().toInt()
        }
    }
}

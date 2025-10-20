package com.romreviewertools.downitup.data.file

/**
 * Handle for writing to a file
 */
interface FileHandle {
    /**
     * Write bytes to the file
     */
    suspend fun write(bytes: ByteArray)

    /**
     * Write bytes from offset with length
     */
    suspend fun write(bytes: ByteArray, offset: Int, length: Int)

    /**
     * Seek to position in file (for resuming)
     */
    suspend fun seek(position: Long)

    /**
     * Get current file size
     */
    suspend fun size(): Long

    /**
     * Close the file
     */
    suspend fun close()

    /**
     * Delete the file
     */
    suspend fun delete()
}

/**
 * Platform-specific file writer for downloading files
 */
expect fun createFileWriter(): FileWriter

expect class FileWriter() {
    /**
     * Create a file writer for the given path
     * Creates parent directories if they don't exist
     */
    suspend fun createWriter(path: String): FileHandle

    /**
     * Get the default downloads directory for the platform
     */
    fun getDownloadsDirectory(): String

    /**
     * Check if we have permission to write to the path
     */
    fun canWrite(path: String): Boolean
}

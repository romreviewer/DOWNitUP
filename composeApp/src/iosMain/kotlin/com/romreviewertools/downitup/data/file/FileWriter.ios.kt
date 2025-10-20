package com.romreviewertools.downitup.data.file

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*

actual fun createFileWriter(): FileWriter = FileWriter()

actual class FileWriter actual constructor() {
    actual suspend fun createWriter(path: String): FileHandle {
        val nsString = path as NSString
        val fileManager = NSFileManager.defaultManager

        // Create parent directories
        val parentPath = nsString.stringByDeletingLastPathComponent
        fileManager.createDirectoryAtPath(
            parentPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        // Create file if it doesn't exist
        if (!fileManager.fileExistsAtPath(path)) {
            fileManager.createFileAtPath(path, contents = null, attributes = null)
        }

        return IosFileHandle(path)
    }

    actual fun getDownloadsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDownloadsDirectory,
            NSUserDomainMask,
            true
        )
        val documentsPath = paths.firstOrNull() as? String
            ?: NSHomeDirectory() + "/Downloads"

        // Ensure directory exists
        NSFileManager.defaultManager.createDirectoryAtPath(
            documentsPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        return documentsPath
    }

    actual fun canWrite(path: String): Boolean {
        return try {
            val nsString = path as NSString
            val parentPath = nsString.stringByDeletingLastPathComponent
            NSFileManager.defaultManager.isWritableFileAtPath(parentPath)
        } catch (e: Exception) {
            false
        }
    }
}

class IosFileHandle(private val path: String) : FileHandle {
    private var fileHandle: NSFileHandle? = null

    private fun ensureOpen() {
        if (fileHandle == null) {
            fileHandle = NSFileHandle.fileHandleForWritingAtPath(path)
                ?: NSFileHandle.fileHandleForUpdatingAtPath(path)
        }
    }

    override suspend fun write(bytes: ByteArray) {
        ensureOpen()
        val data = bytes.toNSData()
        fileHandle?.writeData(data)
    }

    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
        ensureOpen()
        val subArray = bytes.copyOfRange(offset, offset + length)
        val data = subArray.toNSData()
        fileHandle?.writeData(data)
    }

    override suspend fun seek(position: Long) {
        ensureOpen()
        fileHandle?.seekToFileOffset(position.toULong())
    }

    override suspend fun size(): Long {
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
        return (attributes?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
    }

    override suspend fun close() {
        fileHandle?.closeFile()
        fileHandle = null
    }

    override suspend fun delete() {
        close()
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }

    private fun ByteArray.toNSData(): NSData {
        return NSData.create(bytes = this.refTo(0), length = this.size.toULong())
    }
}

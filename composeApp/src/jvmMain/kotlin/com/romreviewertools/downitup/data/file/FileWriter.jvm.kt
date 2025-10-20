package com.romreviewertools.downitup.data.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

actual fun createFileWriter(): FileWriter = FileWriter()

actual class FileWriter actual constructor() {
    actual suspend fun createWriter(path: String): FileHandle {
        return withContext(Dispatchers.IO) {
            val file = File(path)

            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()

            // Create the file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile()
            }

            JvmFileHandle(file)
        }
    }

    actual fun getDownloadsDirectory(): String {
        // Use user's Downloads folder on Windows/Linux/Mac
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("win") -> {
                // Windows: C:\Users\Username\Downloads
                File(userHome, "Downloads").absolutePath
            }
            os.contains("mac") -> {
                // macOS: /Users/Username/Downloads
                File(userHome, "Downloads").absolutePath
            }
            else -> {
                // Linux: /home/username/Downloads
                File(userHome, "Downloads").absolutePath
            }
        }.also {
            // Ensure directory exists
            File(it).mkdirs()
        }
    }

    actual fun canWrite(path: String): Boolean {
        val file = File(path)
        return try {
            // Check if parent directory exists and is writable
            val parent = file.parentFile ?: return false
            parent.exists() && parent.canWrite() || parent.mkdirs()
        } catch (e: Exception) {
            false
        }
    }
}

class JvmFileHandle(private val file: File) : FileHandle {
    private var randomAccessFile: RandomAccessFile? = null

    private fun ensureOpen() {
        if (randomAccessFile == null) {
            randomAccessFile = RandomAccessFile(file, "rw")
        }
    }

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            ensureOpen()
            randomAccessFile?.write(bytes)
        }
    }

    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
        withContext(Dispatchers.IO) {
            ensureOpen()
            randomAccessFile?.write(bytes, offset, length)
        }
    }

    override suspend fun seek(position: Long) {
        withContext(Dispatchers.IO) {
            ensureOpen()
            randomAccessFile?.seek(position)
        }
    }

    override suspend fun size(): Long {
        return withContext(Dispatchers.IO) {
            file.length()
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            randomAccessFile?.close()
            randomAccessFile = null
        }
    }

    override suspend fun delete() {
        withContext(Dispatchers.IO) {
            close()
            file.delete()
        }
    }
}

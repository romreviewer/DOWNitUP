package com.romreviewertools.downitup.data.file

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

actual fun createFileWriter(): FileWriter = FileWriter()

actual class FileWriter actual constructor() {
    actual suspend fun createWriter(path: String): FileHandle {
        return withContext(Dispatchers.IO) {
            val file = File(path)
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
            }
            AndroidFileHandle(file)
        }
    }

    actual fun getDownloadsDirectory(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .absolutePath
            .also { File(it).mkdirs() }
    }

    actual fun canWrite(path: String): Boolean {
        val file = File(path)
        return try {
            val parent = file.parentFile ?: return false
            parent.exists() && parent.canWrite() || parent.mkdirs()
        } catch (e: Exception) {
            false
        }
    }
}

class AndroidFileHandle(private val file: File) : FileHandle {
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

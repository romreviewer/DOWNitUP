package com.romreviewertools.downitup.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Utility functions for URL and filename handling
 */
object UrlUtils {

    /**
     * Extract filename from URL path
     * Examples:
     * - "https://example.com/file.zip" -> "file.zip"
     * - "https://example.com/path/to/document.pdf" -> "document.pdf"
     * - "https://example.com/download?id=123" -> "download"
     */
    fun extractFilenameFromUrl(url: String): String? {
        return try {
            val urlObj = Url(url)
            val path = urlObj.encodedPath

            // Get the last segment of the path
            val lastSegment = path.split('/').lastOrNull { it.isNotBlank() }

            // Remove query parameters if any
            lastSegment?.split('?')?.firstOrNull()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract filename from Content-Disposition header
     * Examples:
     * - "attachment; filename=\"file.zip\"" -> "file.zip"
     * - "attachment; filename*=UTF-8''file%20name.pdf" -> "file name.pdf"
     */
    fun extractFilenameFromContentDisposition(contentDisposition: String?): String? {
        if (contentDisposition == null) return null

        return try {
            // Try filename*= first (RFC 5987 - UTF-8 encoded)
            val filenameStarRegex = """filename\*=(?:UTF-8''|utf-8'')(.+)""".toRegex()
            filenameStarRegex.find(contentDisposition)?.groupValues?.get(1)?.let { encoded ->
                return decodeUrlEncoded(encoded).trim('"', '\'')
            }

            // Try regular filename=
            val filenameRegex = """filename=["']?([^"';]+)["']?""".toRegex()
            filenameRegex.find(contentDisposition)?.groupValues?.get(1)?.trim('"', '\'')
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch filename from HTTP headers using HEAD request
     * Tries Content-Disposition header first, falls back to URL extraction
     */
    suspend fun fetchFilenameFromUrl(httpClient: HttpClient, url: String): String {
        return try {
            val response: HttpResponse = httpClient.head(url)

            // Try to get filename from Content-Disposition header
            val contentDisposition = response.headers[HttpHeaders.ContentDisposition]
            extractFilenameFromContentDisposition(contentDisposition)
                ?: extractFilenameFromUrl(url)
                ?: "download"
        } catch (e: Exception) {
            // If HEAD request fails, fall back to URL extraction
            extractFilenameFromUrl(url) ?: "download"
        }
    }

    /**
     * Simple URL decode for filename extraction
     */
    private fun decodeUrlEncoded(encoded: String): String {
        return encoded.replace("%20", " ")
            .replace("%22", "\"")
            .replace("%27", "'")
            .replace("%2F", "/")
            .replace("%5C", "\\")
            // Add more as needed
    }

    /**
     * Validate and sanitize filename for file system
     * Removes or replaces invalid characters
     */
    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[<>:\"/\\\\|?*]"), "_") // Replace invalid chars with underscore
            .trim()
            .take(255) // Max filename length on most systems
            .ifBlank { "download" }
    }
}

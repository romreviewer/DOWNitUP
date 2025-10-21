package com.romreviewertools.downitup.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romreviewertools.downitup.data.file.createFileWriter
import com.romreviewertools.downitup.di.createHttpClient
import com.romreviewertools.downitup.ui.downloads.DownloadsViewModel
import com.romreviewertools.downitup.util.UrlUtils
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(
    viewModel: DownloadsViewModel? = null
) {
    val fileWriter = remember { createFileWriter() }
    val downloadsDir = remember { fileWriter.getDownloadsDirectory() }
    val httpClient = remember { createHttpClient() }
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var isFilenameAutoFilled by remember { mutableStateOf(false) }
    var isFetchingFilename by remember { mutableStateOf(false) }
    var connectionCount by remember { mutableStateOf(4) }
    var useMultiConnection by remember { mutableStateOf(true) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // Auto-extract filename when URL changes
    LaunchedEffect(url) {
        if (url.isNotBlank() && (filename.isBlank() || isFilenameAutoFilled)) {
            // Extract filename from URL immediately
            val extractedFilename = UrlUtils.extractFilenameFromUrl(url)
            if (extractedFilename != null) {
                filename = extractedFilename
                isFilenameAutoFilled = true
            }
        }
    }

    // Sample URLs for testing (all support HTTP Range / multi-connection)
    val sampleUrls = listOf(
        "https://ash-speed.hetzner.com/100MB.bin" to "100MB (Hetzner)",
        "https://ash-speed.hetzner.com/1GB.bin" to "1GB (Hetzner)",
        "https://proof.ovh.net/files/100Mb.dat" to "100MB (OVH)",
        "https://proof.ovh.net/files/1Gb.dat" to "1GB (OVH)",
        "http://ipv4.download.thinkbroadband.com/100MB.zip" to "100MB (ThinkBroadband)",
        "http://ipv4.download.thinkbroadband.com/512MB.zip" to "512MB (ThinkBroadband)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Add Download",
            style = MaterialTheme.typography.headlineMedium
        )

        // Download directory info
        Text(
            text = "Files will be saved to: $downloadsDir",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Divider()

        // URL Input
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Download URL") },
            placeholder = { Text("https://example.com/file.zip") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Filename Input with auto-fetch button
        OutlinedTextField(
            value = filename,
            onValueChange = {
                filename = it
                isFilenameAutoFilled = false // User manually edited
            },
            label = { Text("Filename (auto-detected)") },
            placeholder = { Text("my-file.zip") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (url.isNotBlank() && !isFetchingFilename) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isFetchingFilename = true
                                try {
                                    val fetchedFilename = UrlUtils.fetchFilenameFromUrl(httpClient, url)
                                    filename = UrlUtils.sanitizeFilename(fetchedFilename)
                                    isFilenameAutoFilled = true
                                } catch (e: Exception) {
                                    snackbarMessage = "Failed to fetch filename: ${e.message}"
                                    showSnackbar = true
                                } finally {
                                    isFetchingFilename = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Fetch filename from server")
                    }
                } else if (isFetchingFilename) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            supportingText = {
                Text(
                    text = if (isFilenameAutoFilled) "Auto-detected from URL" else "Enter filename or click refresh to fetch from server",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        // Multi-Connection Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Multi-Connection Download",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Switch(
                        checked = useMultiConnection,
                        onCheckedChange = { useMultiConnection = it }
                    )
                }

                if (useMultiConnection) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Number of Connections:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$connectionCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = connectionCount.toFloat(),
                            onValueChange = { connectionCount = it.toInt() },
                            valueRange = 1f..16f,
                            steps = 15,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "More connections = faster downloads (if server allows)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Download Button
        Button(
            onClick = {
                if (url.isBlank()) {
                    snackbarMessage = "Please enter a URL"
                    showSnackbar = true
                    return@Button
                }

                if (filename.isBlank()) {
                    snackbarMessage = "Please enter a filename"
                    showSnackbar = true
                    return@Button
                }

                // Add download with proper path
                val sanitizedFilename = UrlUtils.sanitizeFilename(filename.trim())
                val filePath = "$downloadsDir/$sanitizedFilename"
                viewModel?.addHttpDownload(
                    url = url.trim(),
                    name = sanitizedFilename,
                    savePath = filePath,
                    connectionCount = connectionCount,
                    useMultiConnection = useMultiConnection
                )

                // Clear inputs
                url = ""
                filename = ""
                isFilenameAutoFilled = false

                snackbarMessage = "Download added!"
                showSnackbar = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel != null
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Download")
        }

        if (viewModel == null) {
            Text(
                text = "⚠️ ViewModel not initialized. Download functionality unavailable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Divider()

        // Sample Downloads Section
        Text(
            text = "Sample Downloads (for testing)",
            style = MaterialTheme.typography.titleMedium
        )

        sampleUrls.forEach { (sampleUrl, sampleName) ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    url = sampleUrl
                    // Don't set filename, let auto-detection handle it
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sampleName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = sampleUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 How to Test",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = """
                        1. Click a sample download or enter your own URL
                        2. Enter a filename for the download
                        3. Click "Start Download"
                        4. Switch to Downloads tab to see progress
                        5. Use Play/Pause/Cancel buttons to control downloads
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Snackbar
    if (showSnackbar) {
        LaunchedEffect(snackbarMessage) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(snackbarMessage)
        }
    }
}

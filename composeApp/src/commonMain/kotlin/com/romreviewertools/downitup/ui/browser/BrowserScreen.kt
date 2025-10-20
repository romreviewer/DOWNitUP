package com.romreviewertools.downitup.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romreviewertools.downitup.data.file.createFileWriter
import com.romreviewertools.downitup.ui.downloads.DownloadsViewModel

@Composable
fun BrowserScreen(
    viewModel: DownloadsViewModel? = null
) {
    val fileWriter = remember { createFileWriter() }
    val downloadsDir = remember { fileWriter.getDownloadsDirectory() }

    var url by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // Sample URLs for testing
    val sampleUrls = listOf(
        "https://ash-speed.hetzner.com/100MB.bin" to "100MB Test File",
        "https://speed.hetzner.de/1GB.bin" to "1GB Test File",
        "https://releases.ubuntu.com/22.04/ubuntu-22.04.3-desktop-amd64.iso" to "Ubuntu 22.04 ISO"
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

        // Filename Input
        OutlinedTextField(
            value = filename,
            onValueChange = { filename = it },
            label = { Text("Filename") },
            placeholder = { Text("my-file.zip") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

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
                val filePath = "$downloadsDir/${filename.trim()}"
                viewModel?.addHttpDownload(
                    url = url.trim(),
                    name = filename.trim(),
                    savePath = filePath
                )

                // Clear inputs
                url = ""
                filename = ""

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
                    filename = sampleName
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

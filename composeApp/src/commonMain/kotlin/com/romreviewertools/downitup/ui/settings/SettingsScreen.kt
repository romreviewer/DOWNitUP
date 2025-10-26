package com.romreviewertools.downitup.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romreviewertools.downitup.data.file.createFileWriter
import com.romreviewertools.downitup.data.settings.SettingsRepository
import com.romreviewertools.downitup.data.settings.createSettings
import com.romreviewertools.downitup.util.PermissionHandler
import com.romreviewertools.downitup.util.createPermissionHandler
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val settingsRepository = remember { SettingsRepository(createSettings()) }
    val permissionHandler = remember { createPermissionHandler() }
    val fileWriter = remember { createFileWriter() }
    val coroutineScope = rememberCoroutineScope()

    val downloadLocation by settingsRepository.downloadLocation.collectAsState()
    val useMultiConnection by settingsRepository.useMultiConnectionDefault.collectAsState()
    val connectionCount by settingsRepository.defaultConnectionCount.collectAsState()

    var permissionGranted by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check permission status on launch
    LaunchedEffect(Unit) {
        permissionGranted = permissionHandler.isStoragePermissionGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        HorizontalDivider()

        // Storage Permission Section
        SettingsSection(
            title = "Storage & Permissions",
            icon = Icons.Default.Lock
        ) {
            // Permission Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = if (permissionGranted) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (permissionGranted) "Storage Access Granted" else "Storage Access Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (permissionGranted)
                                "App can download files to storage"
                            else
                                "Grant permission to download files",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Icon(
                        imageVector = if (permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (!permissionGranted) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            permissionGranted = permissionHandler.requestStoragePermission()
                            if (!permissionGranted) {
                                showPermissionDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Storage Permission")
                }
            }
        }

        HorizontalDivider()

        // Download Location Section
        SettingsSection(
            title = "Download Location",
            icon = Icons.Default.Folder
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current Location",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = downloadLocation ?: fileWriter.getDownloadsDirectory(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = {
                    // TODO: Implement directory picker
                    // For now, show info that this feature is coming
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = permissionGranted
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Change Download Location")
            }

            if (downloadLocation != null) {
                OutlinedButton(
                    onClick = {
                        settingsRepository.setDownloadLocation(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Default")
                }
            }
        }

        HorizontalDivider()

        // Download Settings Section
        SettingsSection(
            title = "Download Defaults",
            icon = Icons.Default.Download
        ) {
            // Multi-connection toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Multi-Connection Downloads",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Enable by default for faster downloads",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = useMultiConnection,
                        onCheckedChange = { settingsRepository.setUseMultiConnectionDefault(it) }
                    )
                }
            }

            // Connection count slider
            if (useMultiConnection) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Default Connections",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$connectionCount",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = connectionCount.toFloat(),
                            onValueChange = { settingsRepository.setDefaultConnectionCount(it.toInt()) },
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

        HorizontalDivider()

        // About Section
        SettingsSection(
            title = "About",
            icon = Icons.Default.Info
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DOWNitUP",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "A Kotlin Multiplatform download manager with torrent and HTTP support",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Storage Permission Required") },
            text = {
                Text("This app needs storage permission to download files. Please grant the permission in your device settings.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionHandler.openAppSettings()
                        showPermissionDialog = false
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        }

        content()
    }
}

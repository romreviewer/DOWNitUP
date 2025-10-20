# DOWNitUP - Solution Document
## Torrent + HTTP File Downloader - Kotlin Multiplatform

### Project Overview
A cross-platform (Android, iOS, Desktop) file downloader supporting both HTTP and Torrent protocols with streaming capabilities.

> **📚 IMPORTANT**: See `TORRENT_LIBRARY_RESEARCH.md` for comprehensive analysis of torrent library options, trade-offs, and recommendations. This document provides the implementation roadmap based on that research.

---

## ✅ Implementation Status

### Completed Phases:

#### ✅ Phase 1: Project Setup & Navigation (COMPLETE)
- Jetpack Navigation Compose 2.9.1 (stable)
- Bottom navigation with Downloads & Browser tabs
- Material 3 icons (CloudDownload, TravelExplore)
- Working navigation between screens

#### ✅ Phase 2: Database Layer (COMPLETE)
- **SQLDelight 2.0.2** (switched from Room for stability)
- Download entity with 17 fields
- 15+ SQL queries for CRUD operations
- Platform-specific database drivers (Android, iOS, Desktop)
- Repository pattern implementation

#### ✅ Phase 3: HTTP Download Manager (COMPLETE)
- **Ktor Client** for HTTP downloads
- **Resumable downloads** with Range headers
- **Real-time progress tracking** (updates every 500ms)
- **Download speed calculation**
- **Pause/Resume/Cancel** functionality
- **Actual file writing** to disk with platform-specific FileWriter
- **8KB buffer** streaming for memory efficiency
- **Error handling** with database error logging

#### ✅ Phase 3.5: File Management (COMPLETE)
- Platform-specific FileWriter (`expect`/`actual`)
- Automatic downloads directory detection:
  - **Windows**: `C:\Users\Username\Downloads`
  - **macOS**: `/Users/Username/Downloads`
  - **Linux**: `/home/username/Downloads`
  - **Android**: External storage Downloads folder
  - **iOS**: Documents/Downloads directory
- File operations: create, write, seek, close, delete
- Resumable downloads with file seeking
- Automatic parent directory creation

#### ✅ UI Implementation (COMPLETE)
- **Downloads Screen** with full functionality:
  - Download list with progress bars
  - Real-time speed & size display
  - Action buttons (Play/Pause/Cancel/Delete/Retry)
  - Active & Completed download counters
  - Empty state with helpful message
  - Status-based color coding
- **Browser Screen** for adding downloads:
  - URL & filename input fields
  - Sample download URLs for testing
  - Downloads directory display
  - Input validation
  - Success/error messages
- **ViewModel** integration with repository & download manager

### Pending Phases:

#### ⏳ Phase 4: Torrent Download Manager
- Platform-specific torrent implementations
- Android: TorrentStream-Android
- Desktop: Anitorrent
- iOS: Custom implementation

#### ⏳ Phase 5: Advanced Features
- Background downloads
- Notifications
- Queue management
- Bandwidth limiting

---

## Architecture Overview

### UI Layer
- **Navigation**: Jetpack Navigation Compose (Multiplatform)
- **Layout**: Bottom Navigation with Material 3 icons
- **Bottom Navigation Tabs**:
  1. **Downloads** - CloudDownload icon (☁️📥) - Shows active + completed downloads
  2. **Browser** - TravelExplore icon (🌐🔍) - Add new downloads (HTTP/Torrent URLs)

### Data Layer
- **Local Database**: Room KMP for download metadata
- **Download Engines**:
  - HTTP: Ktor Client with resumable downloads
  - Torrent: Platform-specific implementations (see TORRENT_LIBRARY_RESEARCH.md)
    - Android: TorrentStream-Android or Anitorrent
    - Desktop: Anitorrent
    - iOS: SwiftyTorrent or custom libtorrent bridge

### Domain Layer
- Repository pattern for download management
- Use cases for download operations (add, pause, resume, cancel, delete)

---

## Technology Stack

### Core Dependencies

#### Navigation & UI
```kotlin
// Jetpack Navigation for Compose Multiplatform
androidx-navigation-compose (2.8.0-alpha10)

// Material3 for Bottom Navigation & UI components
compose.material3 (included in Compose Multiplatform 1.9.0)

// Material Icons Extended for navigation icons (CloudDownload, TravelExplore, etc.)
compose.materialIconsExtended (included in Compose Multiplatform 1.9.0)
```

#### Database
```kotlin
// Room KMP (alpha/beta support)
androidx.room:room-runtime (2.7.0-alpha01+)
androidx.room:room-compiler
androidx.sqlite:sqlite-bundled // For KMP support
```

#### Networking
```kotlin
// Ktor for HTTP downloads
io.ktor:ktor-client-core
io.ktor:ktor-client-cio // or platform-specific engines
io.ktor:ktor-client-content-negotiation

// Torrent Libraries (see TORRENT_LIBRARY_RESEARCH.md for details):
// Android: TorrentStream-Android (best streaming) or Anitorrent
// Desktop: Anitorrent (KMP support for JVM)
// iOS: Platform-specific implementation required
```

#### Coroutines & Flow
```kotlin
kotlinx-coroutines-core (already included)
kotlinx-serialization // for torrent file parsing
```

---

## Implementation Roadmap

### Phase 1: Project Setup & Navigation (Week 1)

#### Step 1.1: Add Required Dependencies
Add Serialization plugin to `composeApp/build.gradle.kts`:
```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.kotlinSerialization)
}
```

Update `composeApp/build.gradle.kts` dependencies:
```kotlin
commonMain.dependencies {
    // Compose Material Icons Extended (for navigation icons)
    implementation(compose.materialIconsExtended)

    // Navigation
    implementation(libs.androidx.navigation.compose) // 2.8.0-alpha10

    // Ktor Client (stable versions)
    implementation(libs.ktor.client.core) // 2.3.13
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Serialization
    implementation(libs.kotlinx.serialization.json) // 1.7.3

    // ViewModel & Lifecycle
    // (already included)
}

androidMain.dependencies {
    implementation(libs.ktor.client.okhttp)
}

iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}

jvmMain.dependencies {
    implementation(libs.ktor.client.cio)
}
```

**Note**: Room database will be added in Phase 2. For Phase 1 (Navigation), we're using stable versions of all libraries.

#### Step 1.2: Create Navigation Graph
File: `commonMain/kotlin/com/romreviewertools/downitup/navigation/NavGraph.kt`
```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Downloads.route
    ) {
        composable(Screen.Downloads.route) {
            DownloadsScreen()
        }
        composable(Screen.Browser.route) {
            BrowserScreen()
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Downloads : Screen("downloads", "Downloads", Icons.Default.CloudDownload)
    data object Browser : Screen("browser", "Browser", Icons.Default.TravelExplore)

    companion object {
        val mainScreens = listOf(Downloads, Browser)
    }
}
```

#### Step 1.3: Create Main Screen with Bottom Navigation
File: `commonMain/kotlin/com/romreviewertools/downitup/ui/MainScreen.kt`
```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Downloads, Screen.Browser)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavGraph(navController)
        }
    }
}
```

### Phase 2: Database Layer (Week 1-2)

#### Step 2.1: Define Database Entities
File: `commonMain/kotlin/com/romreviewertools/downitup/data/local/entities/DownloadEntity.kt`
```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val downloadType: DownloadType, // HTTP or TORRENT
    val status: DownloadStatus, // QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
    val totalBytes: Long,
    val downloadedBytes: Long,
    val downloadSpeed: Long, // bytes per second
    val savePath: String,
    val createdAt: Long,
    val completedAt: Long? = null,
    val error: String? = null,
    // Torrent specific
    val infoHash: String? = null,
    val seeders: Int = 0,
    val peers: Int = 0
)

enum class DownloadType {
    HTTP, TORRENT
}

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}
```

#### Step 2.2: Create DAO
File: `commonMain/kotlin/com/romreviewertools/downitup/data/local/dao/DownloadDao.kt`
```kotlin
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses)")
    fun getDownloadsByStatus(statuses: List<DownloadStatus>): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)
}
```

#### Step 2.3: Create Database
File: `commonMain/kotlin/com/romreviewertools/downitup/data/local/AppDatabase.kt`
```kotlin
@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}

// Platform-specific builders using expect/actual
expect object DatabaseBuilder {
    fun build(): AppDatabase
}
```

Platform implementations in respective source sets.

### Phase 3: Download Engine - HTTP (Week 2)

#### Step 3.1: HTTP Download Manager
File: `commonMain/kotlin/com/romreviewertools/downitup/data/download/HttpDownloadManager.kt`
```kotlin
class HttpDownloadManager(
    private val client: HttpClient,
    private val dao: DownloadDao
) {
    private val activeDownloads = mutableMapOf<Long, Job>()

    suspend fun startDownload(downloadId: Long) {
        val download = dao.getDownloadById(downloadId) ?: return

        activeDownloads[downloadId] = CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadFile(download)
            } catch (e: Exception) {
                handleDownloadError(downloadId, e)
            }
        }
    }

    private suspend fun downloadFile(download: DownloadEntity) {
        // Implement resumable download logic
        // Update progress in database
        // Support chunked downloads
    }

    suspend fun pauseDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
    }

    suspend fun cancelDownload(downloadId: Long) {
        pauseDownload(downloadId)
        // Delete partial files
    }
}
```

### Phase 4: Download Engine - Torrent (Week 3)

#### Step 4.1: Torrent Download Manager
**Research Finding**: No single KMP torrent library supports all platforms. See `TORRENT_LIBRARY_RESEARCH.md` for full analysis.

**Recommended Approach: Hybrid Platform-Specific Implementation**

Add dependencies:
```kotlin
// composeApp/build.gradle.kts

androidMain.dependencies {
    // Option 1: TorrentStream-Android (best for streaming)
    implementation("com.github.TorrentStream:TorrentStream-Android:2.7.0")

    // Option 2: Anitorrent (if consistency with desktop preferred)
    // implementation("org.openani.anitorrent:anitorrent-native:0.2.0")
}

jvmMain.dependencies {
    // Anitorrent for Desktop
    implementation("org.openani.anitorrent:anitorrent-native:0.2.0")
}

// iOS - requires custom implementation (see research doc)
```

**Implementation with expect/actual:**
```kotlin
// commonMain
expect class TorrentDownloadManager {
    suspend fun addTorrent(magnetUri: String, savePath: String): String
    suspend fun startDownload(torrentId: String)
    suspend fun pauseDownload(torrentId: String)
    suspend fun setSequentialDownload(torrentId: String, enabled: Boolean)
    fun getProgress(torrentId: String): Flow<TorrentProgress>
}

// androidMain - Use TorrentStream-Android
actual class TorrentDownloadManager {
    private val torrentStream = TorrentStream.init(torrentOptions)

    actual suspend fun addTorrent(magnetUri: String, savePath: String): String {
        return torrentStream.startStream(magnetUri)
    }

    actual suspend fun setSequentialDownload(torrentId: String, enabled: Boolean) {
        // TorrentStream handles this automatically for streaming
    }

    // ... implementation details
}

// jvmMain - Use Anitorrent
actual class TorrentDownloadManager {
    private val session = TorrentSession()

    actual suspend fun addTorrent(magnetUri: String, savePath: String): String {
        val params = AddTorrentParams().apply {
            this.savePath = savePath
            this.url = magnetUri
        }
        val handle = session.addTorrent(params)
        return handle.infoHash()
    }

    actual suspend fun setSequentialDownload(torrentId: String, enabled: Boolean) {
        val handle = session.findTorrent(torrentId)
        handle.setSequentialDownload(enabled)
    }

    // ... implementation details
}

// iosMain - Custom implementation or defer to Phase 2
actual class TorrentDownloadManager {
    // TODO: Implement using SwiftyTorrent or native libtorrent bridge
    // For MVP, can throw NotImplementedError or use HTTP-only
}
```

**Phased Approach (Recommended for MVP):**
1. **Phase 4A**: HTTP downloads only (all platforms) - Week 3
2. **Phase 4B**: Android torrents (TorrentStream) - Week 5
3. **Phase 4C**: Desktop torrents (Anitorrent) - Week 6
4. **Phase 4D**: iOS torrents (custom) - Week 7-8 or defer to v2.0

#### Step 4.2: Torrent File Parser
File: `commonMain/kotlin/com/romreviewertools/downitup/data/torrent/TorrentParser.kt`
```kotlin
class TorrentParser {
    fun parseTorrentFile(data: ByteArray): TorrentMetadata {
        // Parse .torrent file using bencode
        // Extract info hash, file list, trackers
    }

    fun parseMagnetLink(magnetUri: String): TorrentMetadata {
        // Parse magnet URI
        // Extract info hash and trackers
    }
}

data class TorrentMetadata(
    val infoHash: String,
    val name: String,
    val totalSize: Long,
    val files: List<TorrentFile>,
    val trackers: List<String>
)
```

### Phase 5: Repository & Domain Layer (Week 3-4)

#### Step 5.1: Download Repository
File: `commonMain/kotlin/com/romreviewertools/downitup/domain/repository/DownloadRepository.kt`
```kotlin
interface DownloadRepository {
    fun getAllDownloads(): Flow<List<Download>>
    fun getActiveDownloads(): Flow<List<Download>>
    fun getCompletedDownloads(): Flow<List<Download>>
    suspend fun addHttpDownload(url: String, savePath: String): Long
    suspend fun addTorrentDownload(torrentData: ByteArray, savePath: String): Long
    suspend fun addMagnetDownload(magnetUri: String, savePath: String): Long
    suspend fun startDownload(id: Long)
    suspend fun pauseDownload(id: Long)
    suspend fun resumeDownload(id: Long)
    suspend fun cancelDownload(id: Long)
    suspend fun deleteDownload(id: Long, deleteFiles: Boolean)
}

class DownloadRepositoryImpl(
    private val downloadDao: DownloadDao,
    private val httpManager: HttpDownloadManager,
    private val torrentManager: TorrentDownloadManager
) : DownloadRepository {
    // Implementation
}
```

#### Step 5.2: Use Cases
File: `commonMain/kotlin/com/romreviewertools/downitup/domain/usecases/`
```kotlin
// AddDownloadUseCase.kt
class AddDownloadUseCase(private val repository: DownloadRepository)

// ManageDownloadUseCase.kt
class ManageDownloadUseCase(private val repository: DownloadRepository)

// GetDownloadsUseCase.kt
class GetDownloadsUseCase(private val repository: DownloadRepository)
```

### Phase 6: UI Implementation (Week 4-5)

#### Step 6.1: Downloads Screen
File: `commonMain/kotlin/com/romreviewertools/downitup/ui/downloads/DownloadsScreen.kt`
```kotlin
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = viewModel { DownloadsViewModel() }
) {
    val downloads by viewModel.downloads.collectAsState()
    val activeDownloads = downloads.filter { it.status == DownloadStatus.DOWNLOADING }
    val completedDownloads = downloads.filter { it.status == DownloadStatus.COMPLETED }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            count = 2,
            state = rememberPagerState()
        ) { page ->
            when (page) {
                0 -> DownloadsList(
                    downloads = activeDownloads,
                    onPause = viewModel::pauseDownload,
                    onResume = viewModel::resumeDownload,
                    onCancel = viewModel::cancelDownload
                )
                1 -> DownloadsList(
                    downloads = completedDownloads,
                    onDelete = viewModel::deleteDownload,
                    onOpen = viewModel::openFile
                )
            }
        }
    }
}

@Composable
fun DownloadItem(
    download: Download,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(download.name, style = MaterialTheme.typography.titleMedium)

            LinearProgressIndicator(
                progress = download.progress,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${download.downloadedBytes.formatBytes()} / ${download.totalBytes.formatBytes()}")
                Text("${download.downloadSpeed.formatSpeed()}")
            }

            // Action buttons
            Row {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> IconButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, "Pause")
                    }
                    DownloadStatus.PAUSED -> IconButton(onClick = onResume) {
                        Icon(Icons.Default.PlayArrow, "Resume")
                    }
                    else -> {}
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "Cancel")
                }
            }
        }
    }
}
```

#### Step 6.2: ViewModel
File: `commonMain/kotlin/com/romreviewertools/downitup/ui/downloads/DownloadsViewModel.kt`
```kotlin
class DownloadsViewModel(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val manageDownloadUseCase: ManageDownloadUseCase
) : ViewModel() {

    val downloads: StateFlow<List<Download>> = getDownloadsUseCase
        .invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun pauseDownload(id: Long) {
        viewModelScope.launch {
            manageDownloadUseCase.pause(id)
        }
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            manageDownloadUseCase.resume(id)
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            manageDownloadUseCase.cancel(id)
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            manageDownloadUseCase.delete(id, deleteFiles = true)
        }
    }
}
```

#### Step 6.3: Browser/Add Download Screen
File: `commonMain/kotlin/com/romreviewertools/downitup/ui/browser/BrowserScreen.kt`
```kotlin
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel { BrowserViewModel() }
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // URL input
        OutlinedTextField(
            value = viewModel.url,
            onValueChange = { viewModel.url = it },
            label = { Text("Enter URL or Magnet Link") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.addDownload() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Download")
        }

        // File picker button
        Button(
            onClick = { viewModel.pickTorrentFile() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Torrent File")
        }
    }
}
```

### Phase 7: Platform-Specific Features (Week 5-6)

#### Step 7.1: File System Access
Implement expect/actual for:
- File picker
- Storage permissions
- Download directory management
- File opening

#### Step 7.2: Background Downloads
- Android: Foreground Service
- iOS: Background URLSession
- Desktop: Native implementation

#### Step 7.3: Notifications
- Download progress notifications
- Completion notifications
- Error notifications

### Phase 8: Advanced Features (Week 6+)

1. **Download Queue Management**
   - Concurrent download limits
   - Priority queue
   - Bandwidth throttling

2. **Torrent Streaming**
   - Sequential piece downloading
   - Media player integration
   - Stream while downloading

3. **Settings**
   - Download directory
   - Concurrent downloads
   - Speed limits
   - Theme selection

4. **Advanced UI**
   - Search/filter downloads
   - Sorting options
   - Batch operations
   - Statistics dashboard

---

## Testing Strategy

### Unit Tests
- Repository layer
- Use cases
- ViewModels
- Download managers

### Integration Tests
- Database operations
- Download flow end-to-end

### UI Tests
- Navigation flow
- Download item interactions

---

## Deployment Considerations

### Android
- Minimum SDK: 24
- Permissions: INTERNET, WRITE_EXTERNAL_STORAGE (SDK < 29), Foreground Service
- Build: Release APK/AAB with ProGuard

### iOS
- Minimum iOS: 14.0
- Entitlements: Background modes, network access
- Build: Archive for TestFlight/App Store

### Desktop
- Packages: DMG (macOS), MSI (Windows), DEB (Linux)
- JVM bundled distributions

---

## Known Challenges & Solutions

### Challenge 1: Torrent Library for KMP
**Solution**: Use platform-specific implementations with expect/actual pattern. Start with HTTP-only MVP.

### Challenge 2: Background Downloads on iOS
**Solution**: Use URLSession with background configuration, implement download task delegates.

### Challenge 3: File System Permissions
**Solution**: Platform-specific permission handling, use native file pickers.

### Challenge 4: Room KMP Stability
**Solution**: Room KMP is in alpha. Consider SQLDelight as alternative if issues arise.

---

## Timeline Summary

- **Week 1**: Navigation + UI scaffold
- **Week 2**: Database + HTTP downloads
- **Week 3**: Torrent support architecture
- **Week 4**: Repository + Domain layer
- **Week 5**: UI implementation + ViewModels
- **Week 6**: Platform-specific features
- **Week 7+**: Advanced features + polish

---

## Next Steps

1. Review and approve this solution document
2. Set up project dependencies
3. Create feature branches for each phase
4. Start with Phase 1 implementation
5. Iterate based on testing feedback
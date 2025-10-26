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
- **🆕 Multi-Connection Downloads** (Phase 3.6):
  - Parallel chunk downloading (1-16 connections)
  - Automatic server capability detection
  - Smart fallback for unsupported servers
  - Per-connection progress tracking
  - Real-time UI visualization of all connections
  - 4-16x faster downloads (server dependent)

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

#### 🚧 Phase 4: Torrent Download Manager (IN PROGRESS)
- **Library Choice**: **libtorrent4j** (2.1.0-38) - Better documentation than Anitorrent
- **Architecture**: Hybrid platform-specific implementation with `expect`/`actual` pattern
- **Status**: Foundation complete, API integration in progress

**Completed**:
- ✅ `TorrentDownloadManager` expect/actual interface
- ✅ `UnifiedDownloadManager` - Routes HTTP vs Torrent based on download type
- ✅ Torrent data models (`TorrentProgress`, `TorrentMetadata`, `TorrentState`)
- ✅ Magnet link detection and parsing in BrowserScreen
- ✅ UI components for torrent info display
- ✅ Database schema already supports torrent fields
- ✅ Repository `addTorrentDownload()` method
- ✅ ViewModel `addTorrentDownload()` method
- ✅ DI configuration with `UnifiedDownloadManager`
- ✅ libtorrent4j dependencies added for Desktop & Android
- ✅ Desktop (JVM) implementation scaffold created
- ✅ Android implementation scaffold created (libtorrent4j)
- ✅ iOS stub implementation (NotImplementedError)

**In Progress**:
- ⏳ Desktop (JVM) libtorrent4j API integration
  - SessionManager initialization ✅
  - Magnet link download method (API adjustments needed)
  - Progress monitoring implementation
  - State mapping to TorrentState enum

**Remaining**:
- 🔲 Complete Desktop libtorrent4j API integration
- 🔲 Android libtorrent4j implementation
- 🔲 Test torrent downloads end-to-end
- 🔲 Database progress updates for torrents
- 🔲 Sequential download for streaming
- 🔲 iOS implementation (defer to v2.0 or use custom bridge)

**Technology Stack**:
- **Desktop & Android**: org.libtorrent4j:libtorrent4j:2.1.0-38
  - Desktop: Base library + native bindings
  - Android: Base library + arm64/arm architectures
- **iOS**: Deferred (stub implementation)

### Pending Phases:

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
- **Local Database**: SQLDelight 2.0.2 for download metadata
- **Download Engines**:
  - **HTTP**: Ktor Client with resumable downloads & multi-connection support
  - **Torrent**: Platform-specific implementations using libtorrent4j
    - Android: libtorrent4j (arm64, arm)
    - Desktop: libtorrent4j (JVM)
    - iOS: Stub implementation (deferred)
- **UnifiedDownloadManager**: Routes downloads to appropriate manager based on type
  - Automatically detects HTTP vs Torrent from database
  - Delegates operations (start, pause, cancel, delete) to correct engine
  - Provides unified interface for ViewModel

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

### Phase 3: Download Engine - HTTP (Week 2) ✅ COMPLETE

#### Step 3.1: HTTP Download Manager ✅
**Status**: Complete with multi-connection support

**Features Implemented**:
- ✅ Single-connection downloads with resume capability
- ✅ Multi-connection parallel chunk downloading
- ✅ Server capability detection (Range headers support)
- ✅ Intelligent fallback mechanisms
- ✅ Real-time progress aggregation from all chunks
- ✅ Per-chunk speed tracking and UI visualization

**Key Implementation Details**:
- **Database Schema**: Added `download_chunks` table for chunk tracking
- **Connection Management**:
  - Default: 4 connections (configurable 1-16)
  - Minimum file size for chunking: 10 MB
  - Minimum chunk size: 1 MB
- **Algorithm**:
  1. HEAD request to check server Range support
  2. Calculate optimal chunk sizes
  3. Launch parallel coroutines for each chunk
  4. Each chunk writes to specific file position using `seek()`
  5. Progress aggregated every 500ms
  6. UI displays real-time per-connection progress

**File**: `commonMain/kotlin/com/romreviewertools/downitup/data/manager/HttpDownloadManager.kt`

### Phase 4: Download Engine - Torrent (Week 3) 🚧 IN PROGRESS

#### Step 4.1: Torrent Download Manager
**Research Finding**: No single KMP torrent library supports all platforms. See `TORRENT_LIBRARY_RESEARCH.md` for full analysis.

**Chosen Library: libtorrent4j** (v2.1.0-38)
- Better documentation than Anitorrent
- Active maintenance
- Works on Desktop JVM and Android
- SessionManager API for easy integration

**Implementation Approach: Hybrid Platform-Specific**

Add dependencies:
```kotlin
// gradle/libs.versions.toml
[versions]
libtorrent4j = "2.1.0-38"

[libraries]
libtorrent4j = { module = "org.libtorrent4j:libtorrent4j", version.ref = "libtorrent4j" }
libtorrent4j-android-arm64 = { module = "org.libtorrent4j:libtorrent4j-android-arm64", version.ref = "libtorrent4j" }
libtorrent4j-android-arm = { module = "org.libtorrent4j:libtorrent4j-android-arm", version.ref = "libtorrent4j" }

// composeApp/build.gradle.kts
androidMain.dependencies {
    implementation(libs.libtorrent4j)
    implementation(libs.libtorrent4j.android.arm64)
    implementation(libs.libtorrent4j.android.arm)
}

jvmMain.dependencies {
    implementation(libs.libtorrent4j)
}

// iOS - stub implementation for now
```

**Implementation with expect/actual:**
```kotlin
// commonMain/kotlin/.../data/torrent/TorrentDownloadManager.kt
expect class TorrentDownloadManager() : DownloadManager {
    suspend fun addMagnetLink(downloadId: Long, magnetUri: String, savePath: String): String
    suspend fun addTorrentFile(downloadId: Long, torrentFilePath: String, savePath: String): String
    suspend fun setSequentialDownload(downloadId: Long, enabled: Boolean)
    fun getTorrentProgress(downloadId: Long): Flow<TorrentProgress>
    fun parseMagnetLink(magnetUri: String): TorrentMetadata
    fun isMagnetLink(url: String): Boolean
}

// commonMain/kotlin/.../data/manager/UnifiedDownloadManager.kt
class UnifiedDownloadManager(
    private val httpManager: HttpDownloadManager,
    private val torrentManager: TorrentDownloadManager,
    private val database: AppDatabase
) : DownloadManager {
    override suspend fun startDownload(downloadId: Long) {
        val download = getDownloadType(downloadId)
        when (download?.downloadType) {
            DownloadType.HTTP -> httpManager.startDownload(downloadId)
            DownloadType.TORRENT -> torrentManager.startDownload(downloadId)
            null -> println("Download $downloadId not found")
        }
    }
    // ... routes all operations to appropriate manager
}

// jvmMain - libtorrent4j implementation
actual class TorrentDownloadManager actual constructor() : DownloadManager {
    private val sessionManager: SessionManager
    private val activeTorrents = mutableMapOf<Long, TorrentHandle>()

    init {
        sessionManager = object : SessionManager() {
            override fun onBeforeStart() {
                super.onBeforeStart()
                val sp = SessionParams(SettingsPack())
                sp.settings().enableDht(true)  // Enable DHT for magnet links
            }
        }
        sessionManager.start()
    }

    actual suspend fun addMagnetLink(downloadId: Long, magnetUri: String, savePath: String): String {
        val saveDir = File(savePath).parentFile ?: File(savePath)
        val handle = sessionManager.download(magnetUri, saveDir)
        activeTorrents[downloadId] = handle
        startProgressMonitoring(downloadId, handle)
        return handle.infoHash().toHex()
    }

    // ... progress monitoring, pause/resume implementation
}

// androidMain - libtorrent4j implementation
actual class TorrentDownloadManager actual constructor() : DownloadManager {
    private val sessionManager: SessionManager
    // Similar to JVM implementation but may have Android-specific optimizations
}

// iosMain - Stub for MVP
actual class TorrentDownloadManager actual constructor() : DownloadManager {
    actual suspend fun addMagnetLink(...): String {
        throw NotImplementedError("Torrent downloads not yet supported on iOS")
    }
}
```

**Data Models:**
```kotlin
// commonMain/kotlin/.../data/torrent/TorrentModels.kt
@Serializable
data class TorrentProgress(
    val downloadId: Long,
    val progress: Float,  // 0.0 to 1.0
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val numSeeds: Int,
    val numPeers: Int,
    val state: TorrentState
)

enum class TorrentState {
    CHECKING_FILES,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    FINISHED,
    SEEDING,
    ALLOCATING,
    CHECKING_RESUME_DATA
}

data class TorrentMetadata(
    val infoHash: String,
    val name: String?,
    val totalSize: Long,
    val trackers: List<String> = emptyList()
)
```

**Phased Approach (Updated for MVP):**
1. **Phase 4A**: HTTP downloads only (all platforms) - ✅ COMPLETE
2. **Phase 4B**: Torrent architecture & UI - ✅ COMPLETE
   - UnifiedDownloadManager routing
   - Magnet link detection in BrowserScreen
   - Torrent data models and database schema
   - ViewModel and Repository integration
3. **Phase 4C**: Desktop torrents (libtorrent4j) - 🚧 IN PROGRESS (90% complete)
   - Dependencies added ✅
   - SessionManager initialization ✅
   - API integration adjustments needed
4. **Phase 4D**: Android torrents (libtorrent4j) - ⏳ PENDING
5. **Phase 4E**: iOS torrents - ⏳ DEFERRED to v2.0 (stub implementation ready)

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

## Current Implementation Status (Latest Update)

### What's Working Right Now ✅
1. **HTTP Downloads** - Fully functional with multi-connection support
   - Downloads work on Desktop, Android, iOS
   - Resume, pause, cancel, delete all working
   - Real-time progress tracking
   - Speed calculations
   - Multi-connection (1-16 parallel connections)

2. **Torrent Architecture** - Complete foundation
   - Magnet link detection in UI ✅
   - Beautiful torrent info cards showing metadata ✅
   - Database schema supports torrents ✅
   - Repository `addTorrentDownload()` method ✅
   - ViewModel integration complete ✅
   - UnifiedDownloadManager routing ✅

3. **UI** - Polished and functional
   - Downloads screen with progress bars
   - Browser screen with URL input and validation
   - Magnet link auto-detection with visual feedback
   - Dynamic UI based on download type

### What's In Progress 🚧
1. **Desktop Torrent Implementation** (90% complete)
   - libtorrent4j integrated
   - SessionManager initialized
   - Needs API method adjustments:
     - `sessionManager.fetchMagnet()` for magnet links
     - Correct method signatures for TorrentHandle API
     - State enum mapping fixes

2. **Android Torrent Implementation** (Dependencies ready)
   - libtorrent4j dependencies added (arm64, arm)
   - Implementation scaffold created
   - Needs similar API adjustments as Desktop

### Next Immediate Steps 🎯
1. Fix libtorrent4j API calls in Desktop implementation
   - Use `fetchMagnet()` instead of `download()` for magnet URIs
   - Adjust TorrentHandle method calls
   - Fix TorrentStatus.State mappings
2. Test Desktop torrent download end-to-end
3. Implement Android TorrentDownloadManager
4. Add database progress updates for torrents
5. Test complete flow: Add magnet → Download → Track progress → Complete

### Files Created/Modified Today 📁
**New Files:**
- `TorrentDownloadManager.kt` (commonMain) - expect interface
- `TorrentModels.kt` (commonMain) - data models
- `UnifiedDownloadManager.kt` (commonMain) - routing manager
- `TorrentDownloadManager.jvm.kt` - Desktop implementation
- `TorrentDownloadManager.android.kt` - Android implementation
- `TorrentDownloadManager.ios.kt` - iOS stub

**Modified Files:**
- `AppDependencies.kt` - Added UnifiedDownloadManager
- `DownloadsViewModel.kt` - Added `addTorrentDownload()`
- `BrowserScreen.kt` - Magnet link detection & UI
- `Download.sq` - Added `updateTorrentProgress` query
- `build.gradle.kts` - libtorrent4j dependencies
- `libs.versions.toml` - libtorrent4j version catalog

## Next Steps

1. ✅ ~~Review and approve this solution document~~
2. ✅ ~~Set up project dependencies~~
3. ✅ ~~Create feature branches for each phase~~
4. ✅ ~~Start with Phase 1 implementation~~
5. 🚧 Complete libtorrent4j API integration (Desktop)
6. ⏳ Test torrent downloads end-to-end
7. ⏳ Implement Android torrent support
8. ⏳ Phase 5: Advanced features (background downloads, notifications)
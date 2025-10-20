# Torrent Library Research for DOWNitUP
## Comprehensive Analysis - January 2025

---

## Executive Summary

After extensive research, **there is NO single Kotlin Multiplatform library that supports Android, iOS, and Desktop with torrent streaming capabilities**. However, there are several viable approaches with different trade-offs.

### Recommended Approach: Hybrid Solution

**Best Option: Anitorrent (Android + Desktop) + Platform-Specific iOS**

- Use **anitorrent** for Android and Desktop
- Implement iOS separately using **SwiftyTorrent** or native libtorrent bridge
- Handle platform differences through KMP's expect/actual pattern

---

## Detailed Library Analysis

### 1. Anitorrent ⭐ **RECOMMENDED for Android/Desktop**

**GitHub:** https://github.com/open-ani/anitorrent
**Maven Central:** `org.openani.anitorrent:anitorrent-native:0.2.0`

#### Pros ✅
- **Native KMP Support** (Android + Desktop JVM)
- **Published to Maven Central** - Easy installation
- **Based on libtorrent** - Battle-tested C++ library
- **Active Development** - Part of Animeko project
- **Multiple Platform Support:**
  - Desktop: Windows x86_64, macOS x86_64, macOS AArch64, Linux x86_64
  - Android: armeabi-v7a, arm64-v8a, x86, x86_64
- **Handles native library distribution** automatically
- **Type-safe Kotlin API** with proper error handling

#### Cons ❌
- **NO iOS SUPPORT** (critical limitation)
- **Not comprehensive** - Only implements features needed by Animeko
- **Limited documentation** - Relatively new project
- **Beta/Alpha status** - May have stability issues

#### Streaming Support
- Based on libtorrent which supports sequential downloading
- Has `torrent::request_time_critical_pieces()` for streaming
- Can prioritize pieces for time-critical playback

#### Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("anitorrentLibs") {
            from("org.openani.anitorrent:catalog:0.1.0")
        }
    }
}

// build.gradle.kts (Android)
kotlin {
    sourceSets.androidMain.dependencies {
        implementation(anitorrentLibs.anitorrent.native)
    }
}

// build.gradle.kts (Desktop JVM)
kotlin {
    sourceSets.jvmMain.dependencies {
        implementation(anitorrentLibs.anitorrent.native)

        // Platform-specific native library
        val triple = getAnitorrentTriple() // Helper function needed
        if (triple != null) {
            implementation("org.openani.anitorrent:anitorrent-native-desktop:0.2.0:$triple")
        }
    }
}
```

#### Usage Estimate
```kotlin
// Note: API is based on libtorrent structure
val session = TorrentSession()
val params = AddTorrentParams()
params.savePath = "/path/to/save"
params.url = magnetLink // or torrentFile

val handle = session.addTorrent(params)

// For streaming - prioritize pieces sequentially
handle.setSequentialDownload(true)
handle.setPiecePriority(pieceIndex, priority)

// Monitor progress
val status = handle.status()
println("Progress: ${status.progress}")
println("Download Rate: ${status.downloadRate}")
```

---

### 2. libtorrent4j + TorrentStream-Android ⭐ **BEST for Android Streaming**

**GitHub libtorrent4j:** https://github.com/aldenml/libtorrent4j
**GitHub TorrentStream:** https://github.com/TorrentStream/TorrentStream-Android

#### Pros ✅
- **Excellent Streaming Support** - Battle-tested in Popcorn Time
- **Sequential Download** with intelligent piece management
- **Mature & Stable** - Production-ready
- **Comprehensive API** - Full libtorrent wrapper
- **Android-optimized** - Handles lifecycle properly
- **Easy to use** - High-level API for streaming

#### Cons ❌
- **Android/JVM ONLY** - No iOS or KMP support
- **Not multiplatform** - Would need separate iOS solution
- **Larger library size** - Full libtorrent binding

#### Streaming Features ⭐⭐⭐⭐⭐
- **Sequential mode** for in-order piece downloading
- **Piece prioritization** for seeking in videos
- **`setInterestedBytes(int)`** for jumping to specific positions
- **Automatic buffering** management
- **Works with VLC, ExoPlayer, MX Player**

#### Installation (Android)
```kotlin
dependencies {
    implementation("com.github.TorrentStream:TorrentStream-Android:2.7.0")
}
```

#### Usage Example
```kotlin
val torrentOptions = TorrentOptions.Builder()
    .saveLocation(downloadDir)
    .removeFilesAfterStop(false)
    .autoDownload(true)
    .build()

val torrentStream = TorrentStream.init(torrentOptions)

torrentStream.addListener(object : TorrentListener {
    override fun onStreamReady(torrent: Torrent) {
        // Stream is ready, can start playing
        val videoFile = torrent.videoFile
        playVideo(videoFile)
    }

    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
        // Update progress
        println("Seeds: ${status.seeds}, Progress: ${status.progress}")
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        // Handle error
    }
})

torrentStream.startStream("magnet:?xt=urn:...")

// For seeking in video
torrent.setInterestedBytes(seekPositionInBytes)
```

---

### 3. iOS Solutions

#### Option A: SwiftyTorrent
**GitHub:** https://github.com/danylokos/SwiftyTorrent

- Basic torrent client for iOS based on libtorrent
- Built with SwiftUI and Combine
- Requires bridging to Kotlin/Native

#### Option B: iTorrent
**GitHub:** https://github.com/gotzlotz/iTorrent-1

- Full-featured torrent client for iOS 9.3+
- Rewritten in Swift and C++
- Files app support
- Can extract code for library use

#### Option C: Custom libtorrent Bridge
- Build libtorrent for iOS
- Create Objective-C++ wrapper
- Bridge to Kotlin/Native via cinterop

**Complexity:** High ⚠️
**Effort:** 3-4 weeks of development

---

### 4. qBittorrent-Kotlin (Remote API Approach) 🔄 **ALTERNATIVE STRATEGY**

**GitHub:** https://github.com/DrewCarlson/qBittorrent-Kotlin
**Maven:** `org.drewcarlson:qbittorrent-client:$version`

#### Concept
Instead of embedding torrent engine in app, use remote qBittorrent instance via API.

#### Pros ✅
- **True KMP support** - JVM/Android, iOS, macOS/Windows/Linux, JS/NodeJS
- **Automatic authentication** handling
- **Coroutine Flow APIs** for syncing
- **All platforms supported** equally
- **No native library complications**
- **Server handles heavy lifting**

#### Cons ❌
- **Requires external qBittorrent server**
- **Not truly standalone** - needs server setup
- **Network dependency** - requires connectivity to server
- **Streaming complexity** - need to stream from server to device
- **Not suitable for mobile-first apps**

#### Use Cases
- Home media server applications
- Desktop-first applications with mobile remote
- Apps where users already have qBittorrent running
- Enterprise/power-user scenarios

#### Installation
```kotlin
commonMain.dependencies {
    implementation("org.drewcarlson:qbittorrent-client:$version")
}
```

#### Usage
```kotlin
val client = QBittorrentClient(baseUrl = "http://localhost:8080")

// Add torrent
client.addTorrent(magnetUri = "magnet:?xt=...")

// Monitor torrents
client.torrentList()
    .collect { torrents ->
        torrents.forEach { torrent ->
            println("${torrent.name}: ${torrent.progress * 100}%")
        }
    }

// Pause/Resume
client.pauseTorrents(listOf(torrentHash))
client.resumeTorrents(listOf(torrentHash))
```

---

## Recommended Architecture Options

### Option 1: Hybrid Approach (RECOMMENDED) ⭐⭐⭐⭐⭐

**Strategy:** Use different libraries per platform, unified interface

```kotlin
// commonMain
expect class TorrentDownloadManager {
    suspend fun addTorrent(magnetUri: String, savePath: String): String
    suspend fun startDownload(torrentId: String)
    suspend fun pauseDownload(torrentId: String)
    fun getProgress(torrentId: String): Flow<TorrentProgress>
    suspend fun setSequentialDownload(torrentId: String, enabled: Boolean)
}

// androidMain - Use TorrentStream-Android or Anitorrent
actual class TorrentDownloadManager {
    private val torrentStream = TorrentStream.init(options)
    // Implementation using TorrentStream-Android
}

// jvmMain - Use Anitorrent
actual class TorrentDownloadManager {
    private val session = TorrentSession()
    // Implementation using anitorrent
}

// iosMain - Use SwiftyTorrent or custom bridge
actual class TorrentDownloadManager {
    // Implementation using Swift bridge or native libtorrent
}
```

**Pros:**
- Best library for each platform
- Optimal streaming on Android (TorrentStream)
- Full control over implementation
- Can leverage platform-specific optimizations

**Cons:**
- More code to maintain (3 implementations)
- Testing complexity
- Different behaviors per platform
- iOS implementation effort

**Timeline:** 4-6 weeks for full implementation

---

### Option 2: Anitorrent + Manual iOS (Middle Ground) ⭐⭐⭐⭐

**Strategy:** Use Anitorrent for Android/Desktop, build custom iOS

```kotlin
// commonMain
expect class TorrentDownloadManager

// androidMain & jvmMain - Both use Anitorrent
actual class TorrentDownloadManager {
    // Same implementation using anitorrent
}

// iosMain - Custom implementation
actual class TorrentDownloadManager {
    // Custom iOS implementation
}
```

**Pros:**
- Shared code for Android + Desktop
- Single library for 2 platforms
- Only need to implement iOS separately

**Cons:**
- iOS still requires custom work
- Anitorrent may lack some features
- Less mature than TorrentStream for streaming

**Timeline:** 3-4 weeks

---

### Option 3: Remote qBittorrent API (Simplest) ⭐⭐⭐

**Strategy:** Use qBittorrent-Kotlin for all platforms

```kotlin
// commonMain - Works everywhere!
class TorrentDownloadManager(
    private val client: QBittorrentClient
) {
    suspend fun addTorrent(magnetUri: String) {
        client.addTorrent(magnetUri = magnetUri)
    }

    fun getActiveDownloads(): Flow<List<Torrent>> {
        return client.torrentList()
    }
}
```

**Pros:**
- True multiplatform from day one
- No native library issues
- Simple implementation
- All platforms equal

**Cons:**
- Requires external server
- Not standalone application
- Streaming requires additional setup
- Not ideal for mobile-first apps

**Timeline:** 1-2 weeks

---

### Option 4: HTTP Only (MVP First) ⭐⭐⭐⭐

**Strategy:** Launch with HTTP downloads, add torrents later

**Phase 1:** HTTP downloads with Ktor (all platforms)
**Phase 2:** Add torrent support using Option 1 or 2

**Pros:**
- Fastest time to market
- Simpler initial implementation
- Prove core concept first
- Add torrents when needed

**Cons:**
- Missing key feature initially
- May disappoint users expecting torrents

**Timeline:** 2 weeks for MVP, +4 weeks for torrents

---

## Final Recommendation 🎯

### For Production App: **Option 1 - Hybrid Approach**

**Implementation Plan:**

1. **Android:** Use **TorrentStream-Android** (best streaming support)
   ```kotlin
   implementation("com.github.TorrentStream:TorrentStream-Android:2.7.0")
   ```

2. **Desktop (JVM):** Use **Anitorrent** (KMP support)
   ```kotlin
   implementation("org.openani.anitorrent:anitorrent-native:0.2.0")
   ```

3. **iOS:** Use **SwiftyTorrent** or bridge to libtorrent
   - Option A: Fork SwiftyTorrent, create Kotlin wrapper
   - Option B: Build custom libtorrent bridge with cinterop
   - Option C: Wait for Anitorrent iOS support (contribute PR?)

### Development Phases:

**Phase 1 (Week 1-2):** HTTP Downloads
- Implement Ktor-based HTTP downloader
- Database, UI, repository layer
- Works on all platforms

**Phase 2 (Week 3-4):** Android Torrents
- Integrate TorrentStream-Android
- Implement expect/actual pattern
- Test streaming functionality

**Phase 3 (Week 4-5):** Desktop Torrents
- Integrate Anitorrent for JVM
- Share common interface with Android
- Test on Windows/macOS/Linux

**Phase 4 (Week 6-8):** iOS Torrents
- Evaluate SwiftyTorrent integration
- OR build custom libtorrent bridge
- Implement iOS actual class
- Test streaming on iOS

**Phase 5 (Week 8+):** Polish & Advanced Features
- Unified streaming API
- Advanced queue management
- Performance optimization

---

## Streaming Implementation Notes

### Sequential Download for Streaming

All recommended libraries support sequential downloading:

**Anitorrent (libtorrent):**
```kotlin
handle.setSequentialDownload(true)
handle.setPiecePriority(pieceIndex, 7) // High priority
```

**TorrentStream-Android:**
```kotlin
torrentStream.startStream(magnetUri)
// Automatically handles sequential download for streaming

// For seeking
torrent.setInterestedBytes(bytePosition)
```

### Best Practices for Streaming

1. **Piece Priority:** Prioritize beginning pieces first
2. **Buffer Ahead:** Download pieces ahead of playback position
3. **Prebuffering:** Wait for initial buffer before starting playback
4. **Adaptive Streaming:** Adjust based on download speed
5. **File Selection:** In multi-file torrents, select target file

---

## Dependencies Summary

### For Recommended Hybrid Approach:

```kotlin
// composeApp/build.gradle.kts

commonMain.dependencies {
    // HTTP Downloads (all platforms)
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
}

androidMain.dependencies {
    // Torrent for Android - TorrentStream
    implementation("com.github.TorrentStream:TorrentStream-Android:2.7.0")

    // Or use Anitorrent instead:
    // implementation("org.openani.anitorrent:anitorrent-native:0.2.0")
}

jvmMain.dependencies {
    // Torrent for Desktop - Anitorrent
    implementation("org.openani.anitorrent:anitorrent-native:0.2.0")
}

iosMain.dependencies {
    // Will need custom implementation or Swift bridge
    // No direct dependency available
}
```

---

## Risk Assessment

### Technical Risks:

| Risk | Impact | Mitigation |
|------|--------|------------|
| Anitorrent lacks features | Medium | Use TorrentStream-Android instead |
| iOS implementation complex | High | Start with HTTP-only on iOS |
| Streaming quality issues | Medium | Implement adaptive buffering |
| Different behavior per platform | Medium | Extensive cross-platform testing |
| Native library size | Low | Use ProGuard/R8 for Android |

### Timeline Risks:

- iOS torrent implementation may take longer than estimated
- Anitorrent stability issues may require fallback to alternatives
- Platform-specific bugs may delay release

### Mitigation Strategy:

1. Start with HTTP downloads (works everywhere)
2. Add Android torrents next (easiest)
3. Add Desktop torrents (similar to Android with Anitorrent)
4. iOS torrents last (most complex) or defer to v2.0

---

## Conclusion

**There is no perfect single library**, but the **hybrid approach using platform-specific implementations** provides the best user experience on each platform while maintaining code sharing for business logic.

**Start with HTTP downloads for MVP**, then progressively add torrent support per platform based on priority and resources.

For a **mobile-first app targeting Android first**, use **TorrentStream-Android** - it's the most mature streaming solution available.

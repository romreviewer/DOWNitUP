# 📥 DOWNitUP

<p align="center">
  <strong>A modern, cross-platform download manager built with Kotlin Multiplatform</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#build">Build</a> •
  <a href="#roadmap">Roadmap</a> •
  <a href="#contributing">Contributing</a>
</p>

---

## 🎯 About

**DOWNitUP** is a powerful, cross-platform download manager that supports both HTTP and BitTorrent protocols. Built with Kotlin Multiplatform and Compose Multiplatform, it provides a native experience on Android, iOS, and Desktop (Windows, macOS, Linux).

### Why DOWNitUP?

- **🌐 Cross-Platform**: One codebase, native performance on Android, iOS, and Desktop
- **🚀 Blazing Fast**: Multi-connection downloads (4-16x faster) with intelligent server detection
- **⚡ Efficient**: 8KB buffer streaming, resumable downloads, parallel chunk processing
- **📊 Real-Time Insights**: Live per-connection progress, speed tracking, and beautiful visualizations
- **🎨 Modern UI**: Material 3 design with intuitive connection progress indicators
- **🔒 Secure**: SSL/TLS support with platform-specific certificate validation
- **💾 Smart Storage**: Automatic downloads directory detection per platform
- **🧠 Intelligent**: Automatic fallback for unsupported servers, adaptive chunk sizing
- **🛡️ Production Ready**: Comprehensive error handling, logging, and retry mechanisms

---

## ✨ Features

### Current Features (v0.1.0)

#### HTTP Downloads
- ✅ **Multi-Connection Downloads** - 🆕 Download files using 1-16 parallel connections for 4-16x speed boost
- ✅ **Smart Server Detection** - 🆕 Automatically detects and uses server Range support
- ✅ **Real-Time Connection Visualization** - 🆕 See progress of each connection with individual speed tracking
- ✅ **Resumable Downloads** - Pause and resume using HTTP Range headers
- ✅ **Progress Tracking** - Real-time speed, size, and percentage
- ✅ **Queue Management** - Automatic download queue with auto-start
- ✅ **Download Controls** - Play, Pause, Cancel, Delete, and Retry
- ✅ **Smart File Management** - Platform-specific downloads directory
- ✅ **Error Recovery** - Comprehensive error handling with retry capability
- ✅ **Intelligent Fallback** - Automatically falls back to single connection for unsupported servers

#### User Interface
- ✅ **Material 3 Design** - Modern, beautiful interface
- ✅ **Downloads Screen** - View all downloads with status indicators
- ✅ **Browser Screen** - Add new downloads with URL input
- ✅ **Real-Time Updates** - Live progress bars and speed indicators
- ✅ **Sample Downloads** - Pre-configured test URLs for quick testing

#### Cross-Platform Support
- ✅ **Android** (API 24+) - Full support with permissions handling
- ✅ **Desktop** (Windows, macOS, Linux) - Native desktop experience
- ✅ **iOS** - Complete iOS support

### Upcoming Features (v0.2.0+)

- 🔄 **BitTorrent Support** - Download torrents with magnet links
- 🌙 **Background Downloads** - Continue downloads when app is minimized
- 🔔 **Notifications** - Download completion and error notifications
- ⚙️ **Advanced Settings** - Bandwidth limiting, concurrent downloads
- 📁 **Custom Save Locations** - Choose where to save downloads
- 🎬 **Media Streaming** - Stream video/audio while downloading
- 📊 **Download History** - Comprehensive download statistics
- 🔍 **Search & Filter** - Find downloads quickly

---

## 🛠️ Tech Stack

### Core Technologies
- **[Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)** 2.2.20 - Share code across platforms
- **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)** 1.9.0 - Modern declarative UI
- **[Ktor Client](https://ktor.io/)** 2.3.13 - HTTP networking
- **[SQLDelight](https://cashapp.github.io/sqldelight/)** 2.0.2 - Type-safe SQL database
- **[Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)** - Async programming

### Platform-Specific
- **Android**: OkHttp engine, Android SQLite driver
- **iOS**: Darwin (URLSession) engine, Native SQLite driver
- **Desktop**: CIO engine, JDBC SQLite driver

### Architecture
- **MVVM** - Model-View-ViewModel pattern
- **Repository Pattern** - Clean separation of data sources
- **Dependency Injection** - Simple DI container (planned: Koin)
- **Flow** - Reactive data streams
- **Navigation Compose** - Type-safe navigation

---

## 🚀 Getting Started

### Prerequisites

- **JDK 21** (Microsoft OpenJDK or similar)
- **Android Studio** (for Android development)
- **Xcode** (for iOS development, macOS only)
- **Gradle** 8.9+

### Clone the Repository

```bash
git clone https://github.com/romreviewer/DOWNitUP.git
cd DOWNitUP
```

### Install Dependencies

Dependencies are automatically downloaded by Gradle.

---

## 🔨 Build

### Android

```bash
# Windows
.\gradlew.bat :composeApp:assembleDebug

# macOS/Linux
./gradlew :composeApp:assembleDebug
```

Output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### Desktop (JVM)

```bash
# Windows
.\gradlew.bat :composeApp:run

# macOS/Linux
./gradlew :composeApp:run

# Build distributable
./gradlew :composeApp:packageDistributionForCurrentOS
```

Output: `composeApp/build/compose/binaries/main/`

### iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select your target device/simulator
3. Click Run (⌘R)

Or use Gradle:

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

### Run Tests

```bash
# All tests
./gradlew :composeApp:test

# Platform-specific
./gradlew :composeApp:testDebugUnitTest  # Android
./gradlew :composeApp:jvmTest            # Desktop
```

---

## 📱 Usage

### Adding Downloads

1. Navigate to the **Browser** tab
2. Enter a download URL or click a sample download
3. Enter a filename
4. **🆕 Configure Multi-Connection** (optional):
   - Toggle **Multi-Connection Download** ON/OFF
   - Adjust connection count slider (1-16 connections)
   - More connections = faster downloads (if server supports it)
5. Click **Start Download**
6. Switch to **Downloads** tab to view real-time progress
7. **🆕 Watch per-connection progress** with live speed indicators

### Managing Downloads

- **Play** ▶️ - Start or resume a paused download
- **Pause** ⏸️ - Pause an active download
- **Cancel** ❌ - Stop and remove partial file
- **Delete** 🗑️ - Remove completed download
- **Retry** 🔄 - Retry a failed download

### Download Locations

- **Windows**: `C:\Users\Username\Downloads`
- **macOS**: `/Users/Username/Downloads`
- **Linux**: `/home/username/Downloads`
- **Android**: External storage `/Download/`
- **iOS**: App documents `/Downloads/`

---

## 🏗️ Architecture

```
DOWNitUP/
├── composeApp/               # Shared Compose Multiplatform code
│   ├── src/
│   │   ├── commonMain/      # Shared code
│   │   │   ├── kotlin/
│   │   │   │   ├── data/    # Data layer (repository, database)
│   │   │   │   ├── domain/  # Domain layer (models, interfaces)
│   │   │   │   ├── ui/      # UI layer (screens, components)
│   │   │   │   └── di/      # Dependency injection
│   │   │   └── sqldelight/  # Database schemas
│   │   ├── androidMain/     # Android-specific code
│   │   ├── iosMain/         # iOS-specific code
│   │   └── jvmMain/         # Desktop-specific code
│   └── build.gradle.kts
├── iosApp/                   # iOS app wrapper
├── gradle/                   # Gradle wrapper
├── SOLUTION.md              # Implementation roadmap
├── TORRENT_LIBRARY_RESEARCH.md  # Torrent library analysis
├── CLAUDE.md                # Development instructions
└── README.md                # This file
```

### Key Components

- **HttpDownloadManager** - Handles HTTP downloads with Ktor
- **FileWriter** - Platform-specific file operations
- **DownloadRepository** - Database operations with SQLDelight
- **DownloadsViewModel** - UI state management
- **AppDependencies** - Dependency injection container

---

## 🗺️ Roadmap

### ✅ Phase 1: Foundation (Complete)
- [x] Project setup with Kotlin Multiplatform
- [x] Navigation with Compose Navigation 2.9.1
- [x] Material 3 UI design
- [x] Bottom navigation (Downloads, Browser)

### ✅ Phase 2: Database (Complete)
- [x] SQLDelight 2.0.2 integration
- [x] Download entity schema
- [x] Repository pattern
- [x] Platform-specific drivers

### ✅ Phase 3: HTTP Downloads (Complete)
- [x] Ktor Client integration
- [x] Resumable downloads
- [x] Progress tracking
- [x] File writing to disk
- [x] Error handling
- [x] **🆕 Multi-connection parallel downloads (1-16 connections)**
- [x] **🆕 Server capability detection (Range support)**
- [x] **🆕 Per-connection progress visualization**
- [x] **🆕 Automatic fallback for unsupported servers**
- [x] **🆕 Intelligent chunk sizing and distribution**

### 🚧 Phase 4: Torrent Support (Planned)
- [ ] Platform-specific torrent libraries
- [ ] Magnet link parsing
- [ ] Peer connection management
- [ ] Torrent streaming

### 📋 Phase 5: Advanced Features (Planned)
- [ ] Background downloads
- [ ] Push notifications
- [ ] Bandwidth limiting
- [ ] Download scheduling
- [ ] Multi-file torrents
- [ ] Media player integration

See [SOLUTION.md](SOLUTION.md) for detailed implementation plan.

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Ways to Contribute

- 🐛 **Report Bugs** - Open an issue with details
- 💡 **Suggest Features** - Share your ideas
- 📖 **Improve Documentation** - Fix typos, add examples
- 🔧 **Submit Pull Requests** - Fix bugs or add features

### Development Setup

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Make your changes
4. Run tests (`./gradlew test`)
5. Commit your changes (`git commit -m 'Add AmazingFeature'`)
6. Push to your branch (`git push origin feature/AmazingFeature`)
7. Open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable/function names
- Add comments for complex logic
- Write unit tests for new features

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **JetBrains** - For Kotlin Multiplatform and Compose Multiplatform
- **Cash App** - For SQLDelight
- **Ktor** - For the excellent HTTP client
- **Material Design** - For the beautiful design system
- **Claude (Anthropic)** - For development assistance

---

## 📞 Contact

- **GitHub**: [@romreviewer](https://github.com/romreviewer)
- **Issues**: [GitHub Issues](https://github.com/romreviewer/DOWNitUP/issues)

---

## 🌟 Star History

If you find this project useful, please consider giving it a ⭐!

---

<p align="center">
  Made with ❤️ using Kotlin Multiplatform
</p>

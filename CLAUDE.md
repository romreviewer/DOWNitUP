# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DOWNitUP is a Kotlin Multiplatform project using Compose Multiplatform, targeting Android, iOS, and Desktop (JVM). The project is being developed as a **Torrent + HTTP file downloader with streaming capabilities**.

**Package name:** `com.romreviewertools.downitup`

**Important Documentation:**
- `SOLUTION.md` - Complete implementation roadmap with code examples
- `TORRENT_LIBRARY_RESEARCH.md` - Comprehensive analysis of torrent library options for KMP
- See these files for architecture decisions and library choices

## Build Commands

### Android
```bash
# Windows
.\gradlew.bat :composeApp:assembleDebug

# macOS/Linux
./gradlew :composeApp:assembleDebug
```

### Desktop (JVM)
```bash
# Windows
.\gradlew.bat :composeApp:run

# macOS/Linux
./gradlew :composeApp:run
```

### iOS
- Open `iosApp` directory in Xcode and run from there
- Or use IDE run configurations

### Tests
```bash
# Windows
.\gradlew.bat :composeApp:test

# macOS/Linux
./gradlew :composeApp:test
```

## Architecture

### Multiplatform Structure

The project follows the standard Kotlin Multiplatform structure with platform-specific source sets:

- **commonMain** (`composeApp/src/commonMain/kotlin`): Shared code for all platforms, contains the core Compose UI (`App.kt`) and common interfaces (`Platform.kt`)
- **androidMain** (`composeApp/src/androidMain/kotlin`): Android-specific code, including `MainActivity.kt` and Android platform implementations
- **iosMain** (`composeApp/src/iosMain/kotlin`): iOS-specific code, including `MainViewController.kt` and iOS platform implementations
- **jvmMain** (`composeApp/src/jvmMain/kotlin`): Desktop (JVM) specific code, including `main.kt` entry point and JVM platform implementations
- **commonTest** (`composeApp/src/commonTest/kotlin`): Shared test code

### Platform Abstraction Pattern

The project uses the `expect`/`actual` pattern for platform-specific implementations:
- `Platform.kt` in commonMain defines the `expect fun getPlatform()` interface
- Each platform implements this with `actual` declarations in their respective source sets

### Entry Points

- **Android**: `MainActivity.kt:10` - ComponentActivity with edge-to-edge enabled
- **Desktop**: `main.kt:6` - Standard Compose Desktop application window
- **iOS**: `MainViewController.kt:5` - ComposeUIViewController wrapper, called from `iOSApp.swift`

### UI Architecture

All platforms share the same Compose UI defined in `App.kt:24`, which uses Material3 theming. The shared UI is platform-agnostic and uses Compose Multiplatform's resource system.

## Key Configuration

- **Kotlin version**: 2.2.20
- **Compose Multiplatform**: 1.9.0
- **Android SDK**: compileSdk 36, minSdk 24, targetSdk 36
- **JVM Target**: Java 11
- **iOS Targets**: arm64 and Simulator arm64, static framework output
- **Desktop distributions**: DMG (macOS), MSI (Windows), DEB (Linux)
- **Hot Reload**: Enabled via `composeHotReload` plugin (beta)

## Dependencies

Dependencies are managed via version catalogs in `gradle/libs.versions.toml`. Main dependencies:
- Compose Multiplatform runtime, foundation, material3, and UI
- AndroidX Lifecycle (ViewModel and Runtime Compose)
- Kotlinx Coroutines (Swing for JVM)

## Module Structure

Single module project with `:composeApp` containing all platform targets. Desktop application main class is `com.romreviewertools.downitup.MainKt:89`.

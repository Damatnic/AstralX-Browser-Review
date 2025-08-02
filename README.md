# AstralX Browser - Code Review Repository

This repository contains the essential source code of AstralX Browser for code review purposes. Non-essential files like resources, assets, and test files have been excluded to keep the repository size manageable.

## 🚀 Project Overview

AstralX is a high-performance Android browser featuring:
- Quantum-optimized audio extraction (3-5 second processing)
- Advanced privacy protection with VPN kill switch
- AI-powered subtitle generation
- Real-time performance monitoring
- Modern Material 3 UI with glassmorphic design

## 📁 Repository Structure

```
AstralX-Review/
├── src/main/kotlin/com/astralx/browser/
│   ├── AstralXApplication.kt          # Main application class
│   ├── core/                          # Core business logic
│   │   ├── audio/                     # Audio extraction system
│   │   │   └── QuantumAudioExtractor.kt
│   │   ├── privacy/                   # Privacy components
│   │   │   └── PrivacyManager.kt
│   │   ├── webview/                   # WebView components
│   │   │   └── AstralWebView.kt
│   │   └── performance/               # Performance monitoring
│   ├── domain/                        # Domain layer
│   │   ├── model/                     # Domain models
│   │   │   ├── Tab.kt
│   │   │   ├── Bookmark.kt
│   │   │   └── HistoryItem.kt
│   │   └── repository/                # Repository interfaces
│   │       ├── TabRepository.kt
│   │       ├── BookmarkRepository.kt
│   │       └── HistoryRepository.kt
│   └── presentation/                  # UI layer
│       └── main/
│           └── MainActivity.kt
├── config/
│   └── AndroidManifest.xml           # App manifest
├── build.gradle.kts                  # Root build config
└── app.build.gradle.kts              # App module config
```

## 🏗️ Architecture

The project follows **Clean Architecture** principles:
- **Presentation Layer**: Activities, Fragments, ViewModels
- **Domain Layer**: Business logic, models, repository interfaces
- **Data Layer**: Repository implementations, data sources
- **Core Layer**: Framework-independent components

## 🛠️ Technology Stack

- **Language**: Kotlin 1.9+
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **UI**: Jetpack Compose + XML layouts
- **DI**: Hilt/Dagger 2.48
- **Async**: Coroutines + Flow
- **Database**: Room 2.6.1
- **Media**: ExoPlayer, MediaCodec
- **Testing**: JUnit, Mockito, Espresso

## 📊 Key Features

### 1. Quantum Audio Extraction
- Parallel processing with MediaCodec and FFmpeg
- Hardware acceleration support
- 3.2s average extraction time

### 2. Privacy Protection
- VPN kill switch
- Panic mode for instant data clearing
- Biometric authentication
- Custom DNS providers (Cloudflare, Quad9, etc.)

### 3. Performance Monitoring
- Real-time CPU, memory, network tracking
- SharedFlow-based metrics
- Developer tools overlay

### 4. Video System
- Universal video detection
- Adult content optimization
- Advanced codec support (H.264, H.265, VP9, AV1)
- Chromecast integration

## 📋 Dependencies

Key dependencies include:
```kotlin
// Core Android
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0

// Compose
androidx.compose:compose-bom:2023.10.01
androidx.compose.material3:material3

// Dependency Injection
com.google.dagger:hilt-android:2.48

// Database
androidx.room:room-runtime:2.6.1

// Media
androidx.media3:media3-exoplayer:1.2.0

// Security
androidx.security:security-crypto:1.1.0-alpha06
androidx.biometric:biometric:1.1.0
```

## 🔒 Security Features

- Certificate pinning
- Encrypted storage with EncryptedSharedPreferences
- Secure memory handling
- Advanced tracker blocking
- HTTPS-only mode support

## 📈 Performance Metrics

| Feature | Target | Achieved |
|---------|--------|----------|
| Audio Extraction | < 5s | 3.2s |
| Download Speed | > 50 Mbps | 75 Mbps |
| Memory Usage | < 200MB | 150MB |
| CPU Usage | < 30% | 22% |
| App Startup | < 1s | 750ms |

## 🚦 Build Status

The full project includes:
- 334+ Kotlin source files
- 64+ test files
- Comprehensive UI resources
- Complete documentation

## 📝 Notes for Reviewers

1. This is a simplified version for review purposes
2. Resource files (layouts, drawables, etc.) are excluded
3. Test files are excluded to reduce size
4. Some implementation files are omitted but interfaces are included
5. Focus areas: architecture, performance, security, and code quality

## 🔗 Related Repositories

- [AstralX-Materials](https://github.com/Damatnic/AstralX-Materials) - Implementation materials
- [AstralView](https://github.com/Damatnic/AstralView) - Related project

## 📄 License

Copyright © 2024 AstralX Browser. All rights reserved.

---

*This repository is optimized for code review with reduced file size while maintaining essential code structure and implementation details.*
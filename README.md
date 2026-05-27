# Echo (KMP)

> [!IMPORTANT]
> **AI Agent Instructions**: If you are an AI agent working on this repository, you **must** read and follow the [AGENTS.md](./AGENTS.md) operating manual before taking any action.

Echo is an **offline-first Kotlin Multiplatform (KMP) chat application** designed to manage multiple conversations between a user and an AI agent. It demonstrates high-quality state management, local data persistence, and platform-native UI performance.

## 🚀 Key Features

- **Offline-First Architecture**: Powered by Room Multiplatform, all chat data is stored locally and loaded instantly.
- **Zero-Network Bootstrap**: Initial data and media assets are pre-bundled and restored locally on the first launch—no internet required.
- **AI Agent Simulation**: Reactive, debounced AI responses with randomized delays and multi-modal (text/image) content.
- **Platform-Native UI**:
    - **Android**: 100% Jetpack Compose with Material3.
    - **iOS**: 100% SwiftUI with native KMP StateFlow observation.

## 🛠 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.3.10 |
| **Persistence** | Room Multiplatform 2.8.4 |
| **Preferences** | Jetpack DataStore (Multiplatform) |
| **Dependency Injection** | Koin 4.1.1 |
| **Image Loading** | Coil (Android) / SwiftUI native (iOS) |
| **Concurrency** | Kotlin Coroutines |
| **Build System** | Gradle (Version Catalogs) |

## 📖 Documentation Guide

This repository follows a strict architectural harness. Refer to these documents for deep dives:

1.  **[ARCHITECTURE.md](./ARCHITECTURE.md)**: High-level overview of the layers and design patterns.
2.  **[SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md)**: The technical blueprint, UDF principles, and sequence diagrams.
3.  **[ROADMAP.md](./ROADMAP.md)**: The 7-phase implementation plan and task tracker.
4.  **[AGENTS.md](./AGENTS.md)**: The mandatory operating manual for AI agents.

## 🏁 Setup & Installation

### Android
1. Open the project in **Android Studio** (Ladybug or newer).
2. Sync Project with Gradle Files.
3. Run the `androidApp` configuration on an emulator or physical device.

### iOS
1. Ensure you have **Xcode** installed.
2. Open `iosApp/iosApp.xcworkspace` in Xcode.
3. Select a simulator and run the `iosApp` scheme.

## 🤖 Agentic Workflow

This project is built to be **AI-Native**. A specialized Gemini CLI skill is provided to automate the implementation loop according to the project's architectural standards.

**To install the development skill:**
```bash
gemini skills install ./skills/echo-dev.skill --scope workspace
/skills reload
```

---
Built with ❤️ using Kotlin Multiplatform.

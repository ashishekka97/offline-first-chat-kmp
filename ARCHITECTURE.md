# Architecture Overview: Echo Chat App

The Echo Chat App is built using **Kotlin Multiplatform (KMP)** with a focus on **Offline-First** capability, **Zero-Network Bootstrap**, and **Unidirectional Data Flow (UDF)**.

## Architectural Layers

1.  **Shared Module (`shared`):**
    *   **Data:** Room Multiplatform (SQLite) for persistence and Jetpack DataStore for metadata.
    *   **Infrastructure:** Local Asset Manager (Okio) for file I/O and Media Processor for image thumbnailing.
    *   **Domain:** Repositories, Backup Restore Service, and AI Agent logic.
    *   **Presentation:** Shared ViewModels (exposing `StateFlow`).
2.  **Platform UI:**
    *   **Android:** Jetpack Compose.
    *   **iOS:** SwiftUI with native wrappers for KMP State observation.

## Design Patterns

*   **Offline-First:** The Room database is the Single Source of Truth. The UI observes database flows and never holds transient list state.
*   **Zero-Network Bootstrap:** All initial data and images are bundled as physical assets and restored locally on first launch.
*   **MVI/MVVM:** Shared ViewModels manage state and process intents from the platform-specific UI.
*   **Simulated AI:** An asynchronous Agent Service manages simulated, debounced replies with randomized delays and content types.

## Key Workflows

*   **Backup Restore Flow:** On the first launch, a `BackupRestoreService` populates the local Room database and processes bundled image assets (thumbnailing) before updating the DataStore flag.
*   **Chat Flow:** User messages are saved locally first, triggering an immediate UI update, followed by an asynchronous, debounced AI response simulation.

For detailed specifications, component designs, and sequence diagrams, refer to [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md).

# Architecture Overview: Echo Chat App

The Echo Chat App is built using **Kotlin Multiplatform (KMP)** with a focus on **Offline-First** capability and **Unidirectional Data Flow (UDF)**.

## Architectural Layers

1.  **Shared Module (`shared`):**
    *   **Data:** Room Multiplatform (SQLite) for persistence.
    *   **Domain:** Repositories and AI Agent logic.
    *   **Presentation:** Shared ViewModels (exposing `StateFlow`).
2.  **Platform UI:**
    *   **Android:** Jetpack Compose.
    *   **iOS:** SwiftUI.

## Design Patterns

*   **Offline-First:** The Room database is the Single Source of Truth. The UI observes database flows and never holds transient list state.
*   **MVI/MVVM:** Shared ViewModels manage state and process intents from the platform-specific UI.
*   **Simulated AI:** An asynchronous Agent Service manages simulated replies with randomized delays and content types.

## Key Workflows

*   **Restore Flow:** On first launch, a `DatabaseSeeder` populates the local Room database from a JSON "backup" file, including handling for image asset metadata.
*   **Chat Flow:** User messages are saved locally first, triggering an immediate UI update, followed by an asynchronous AI response simulation.

For detailed specifications, component designs, and sequence diagrams, refer to [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md).

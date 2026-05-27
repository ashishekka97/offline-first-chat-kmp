# System Design & Architecture: Echo Chat App

This document serves as the architectural blueprint and system design harness for the Echo Chat App. It dictates how components should be structured and interact across the Kotlin Multiplatform (KMP) project. **All implementation efforts must adhere to these guidelines.**

## 1. High-Level Architecture (KMP Strategy)

The project follows a strict separation of concerns, heavily favoring shared code for business logic and data management, leaving the platform modules purely for UI rendering.

*   **`shared` (commonMain):**
    *   **Data Layer:** Room Multiplatform database, DAOs, **Jetpack DataStore** (for preferences).
    *   **Infrastructure:** **Local Asset Manager** (file I/O) and **Media Processor** (image thumbnailing).
    *   **Domain Layer:** Core business logic, domain models, and Repositories.
    *   **Service Layer:** The simulated AI Agent logic (`AgentService`).
    *   **Presentation Layer (Shared ViewModels):** KMP-compatible ViewModels exposing state (via `StateFlow`) to the UI platforms.
*   **`androidApp` & `iosApp` (Platform UI):**
    *   Pure UI layers (Compose/SwiftUI).
    *   Observe `StateFlow` from shared ViewModels.
    *   Dispatches intents/events to shared ViewModels.
    *   **Splash Logic:** Manages the initial "Restore" vs "Home" routing based on DataStore state.

## 2. Core Principles

1.  **Offline-First (Single Source of Truth):**
    *   The Room Database is the *only* source of truth for chat data.
    *   The UI strictly observes database flows (`Flow<List<Message>>`).
    *   User actions write directly to the database. The resulting database update triggers the UI re-render.
2.  **Zero-Network Bootstrap:**
    *   Initial data is restored from pre-bundled assets.
    *   The app requires zero internet access for its core functionality and initial setup.
3.  **Unidirectional Data Flow (UDF):**
    *   State flows *down* from the Shared ViewModel to the UI.
    *   Events/Intents flow *up* from the UI to the Shared ViewModel.
4.  **Concurrency & Asynchrony:**
    *   All async operations use **Kotlin Coroutines**.

## 3. Component Design

### 3.1 Data Layer (Room & DataStore)
*   **Room Entities:** `ChatEntity`, `MessageEntity`.
*   **DataStore:** Tracks application metadata (e.g., `isRestoreCompleted`).

### 3.2 Infrastructure
*   **`LocalAssetManager`:** Manages internal app storage file operations (using Okio).
*   **`MediaProcessor`:** Handles image downsizing and thumbnail generation to ensure high-performance list rendering and disk efficiency.

### 3.3 Domain / Repository Layer
*   **`ChatRepository`:**
    *   Mediates between ViewModels and the Data Layer.
    *   Orchestrates the "Send Message" and "Create Chat" workflows.
*   **`BackupRestoreService`:**
    *   Coordinates the one-time restoration of bundled JSON data and physical image assets.

### 3.4 AI Agent Service
*   **Responsibility:** Simulates replies (70% text, 30% image) with a 1-2s delay.
*   **Simulation Rules:**
    *   **Trigger:** Every 4-5 user messages.
    *   **Debounce:** The service must debounce rapid user inputs to prevent multiple concurrent "thinking" states.
    *   **Persistence:** Agent replies are saved directly to Room, triggering UI updates.

### 3.5 Presentation Layer (Shared ViewModels)
*   **`HomeViewModel`:** Manages the chat list and global app state.
*   **`ChatDetailViewModel`:** Manages message history and input states.

## 4. Key Workflows

### 4.1 "Send Message & AI Reply" Workflow
1.  **UI Intent:** User sends message.
2.  **Repository:** Saves User Message to Room -> Room triggers UI update.
3.  **Agent Trigger:** Repository checks message count and debounces.
4.  **Agent Simulation:** After 1-2s delay, Agent generates reply.
5.  **Agent Persistence:** Agent reply saved to Room -> Room triggers UI update.

### 4.2 "Initial Restore" Workflow
1.  **App Launch:** Platform Splash Screen checks DataStore.
2.  **Restore Engine:** If `isRestoreCompleted` is false, reads bundled JSON and copies/processes bundled images.
3.  **Completion:** Updates DataStore and routes user to the Home Screen.

## 5. Architectural Diagrams

### 5.1 Component & Data Flow Diagram

```mermaid
graph TD
    subgraph "Platform Specific UI"
        A[Android UI - Compose]
        B[iOS UI - SwiftUI]
    end

    subgraph "Shared KMP Module (commonMain)"
        C[Shared ViewModels]
        
        subgraph "Infrastructure"
            I1[Local Asset Manager]
            I2[Media Processor]
        end

        subgraph "Domain Layer"
            D[Chat Repository]
            E[AI Agent Service]
            RS[Backup Restore Service]
        end
        
        subgraph "Data Layer (Offline First)"
            F[(Room Database)]
            DS[Preferences DataStore]
            G[Chat DAO]
            H[Message DAO]
        end
        
        C -- StateFlow --> A
        C -- StateFlow --> B
        A -- Intents --> C
        B -- Intents --> C
        
        C -- Action --> D
        D -- Action --> E
        
        D -- Read/Write --> G
        D -- Read/Write --> H
        RS -- Write --> G
        RS -- Write --> H
        RS -- Use --> I1
        RS -- Use --> I2
        
        G -- Flow --> C
        H -- Flow --> C
        
        G -- SQL --> F
        H -- SQL --> F
    end
```

### 5.2 "Send Message & AI Reply" Sequence Diagram

```mermaid
sequenceDiagram
    participant UI as Platform UI
    participant VM as Shared ViewModel
    participant Repo as Chat Repository
    participant DB as Room Database
    participant AI as AI Agent Service

    UI->>VM: sendMessage("Hello")
    VM->>Repo: sendMessage("Hello")
    
    rect rgb(200, 215, 235)
        Note over Repo, DB: Immediate Local Save
        Repo->>DB: insertMessage(userMessage)
    end
    
    DB-->>VM: Flow emit(Updated Messages)
    VM-->>UI: StateFlow Update
    
    rect rgb(235, 215, 200)
        Note over Repo, AI: AI Debouncing & Trigger
        Repo->>AI: checkAndTriggerSimulation()
        AI->>AI: delay(1000..2000 ms)
        AI->>DB: insertMessage(agentReply)
    end
    
    DB-->>VM: Flow emit(Updated Messages with AI Reply)
    VM-->>UI: StateFlow Update
```

# Project Roadmap: Echo Chat App

This roadmap outlines the implementation phases and technical tasks for the Echo Chat App. Each task is designed to be atomic, self-contained, and technical in scope.

## Phase 1: Persistence Layer
**Goal:** Establish the core offline-first database structure.
*   [x] **Data Layer: Entities & DAOs**: Define `ChatEntity`, `MessageEntity`, and reactive DAOs for real-time updates.
*   [x] **Data Layer: Platform DB Setup**: Implement `AppDatabase` and platform-specific Room builders for Android and iOS.

## Phase 2: Infrastructure & Media
**Goal:** Implement preferences, file management, and image processing utilities.
*   [x] **Data Layer: Preferences DataStore**: Set up KMP DataStore for tracking application state (e.g., initial restore status).
*   [x] **Shared: Local Asset Manager**: Implement basic file I/O for internal app storage using Okio.
*   [ ] **Shared: Media Processor**: Implement image downsizing and thumbnail generation logic for offline performance.

## Phase 3: Backup & Restore
**Goal:** Implement the zero-network initial data restoration flow.
*   [ ] **Backup: Logic & Parsing**: Implement JSON parsing and resource validation for the initial data backup.
*   [ ] **Backup: Restoration Engine**: Coordinate the Asset Manager, Media Processor, and Repository to restore seed data and generate thumbnails.

## Phase 4: AI & Domain Logic
**Goal:** Implement the reactive simulation engine and shared presentation layer.
*   [ ] **Domain: AI Simulation Engine**: Implement randomized response selection and simulated "thinking" delay logic.
*   [ ] **Domain: Agent Lifecycle & Debouncing**: Implement message counting and input debouncing to manage simulation triggers.
*   [ ] **Presentation: Home ViewModel**: Implement the shared view model for chat list management and swipe-to-delete logic.
*   [ ] **Presentation: Chat Detail ViewModel**: Implement the shared view model for message history and auto-scroll triggers.

## Phase 5: Android Platform (Compose)
**Goal:** Build the Android user interface using Jetpack Compose.
*   [ ] **Android: Splash & Startup Routing**: Handle the first-run restore UI vs. immediate application launch.
*   [ ] **Android: Home Screen UI & Interactions**: Build the chat list UI with swipe-to-delete confirmation.
*   [ ] **Android: Chat Detail Message List**: Implement bubble-style message history with auto-scroll behavior.
*   [ ] **Android: Chat Detail Input & Keyboard**: Implement the message input bar with keyboard inset handling.
*   [ ] **Android: Chat Detail Media Rendering**: Integrate Coil for local URI and thumbnail rendering.
*   [ ] **Android: Chat Detail Media Picking**: Implement Gallery and Camera picker intents integrated with the Media Processor.

## Phase 6: iOS Platform (SwiftUI)
**Goal:** Build the iOS user interface using SwiftUI.
*   [ ] **iOS: KMP-Swift Interop Wrappers**: Implement native Swift wrappers for reactive observation of KMP StateFlows.
*   [ ] **iOS: Splash & Startup Routing**: Handle the first-run restore UI vs. immediate application launch.
*   [ ] **iOS: Home Screen UI & Interactions**: Build the chat list UI with native swipe actions and styling.
*   [ ] **iOS: Chat Detail Message List**: Implement the message history view with bubble styling and auto-scroll.
*   [ ] **iOS: Chat Detail Input & Keyboard**: Implement the native input field with keyboard avoiding logic.
*   [ ] **iOS: Media Rendering & Picking**: Implement SwiftUI image loading and native PHPicker/Camera integration.

## Phase 7: Final Polish & Resilience
**Goal:** Ensure UI robustness and cross-platform validation.
*   [ ] **UI: Empty States & Error Harness**: Implement global empty state views and fallback UI for media/database errors.
*   [ ] **Polish: Smart Timestamps & Localization**: Implement localized relative time logic with system 12h/24h support.
*   [ ] **Validation: Offline Persistence & Documentation**: Final verification of offline behavior and update project documentation.

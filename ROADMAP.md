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
*   [x] **Shared: Media Processor**: Implement image downsizing and thumbnail generation logic for offline performance.

## Phase 3: Backup & Restore
**Goal:** Implement the zero-network initial data restoration flow.
*   [x] **Backup: Logic & Parsing**: Implement JSON parsing and resource validation for the initial data backup.
*   [x] **Backup: Restoration Engine**: Coordinate the Asset Manager, Media Processor, and Repository to restore seed data and generate thumbnails.

## Phase 3.5: Architectural Hardening
**Goal:** Formalize the data layer and improve system resilience.
*   [x] **Hardening: Chat & Message Repositories**: Abstract DAOs behind Repositories to act as a Single Source of Truth and handle Entity-to-Domain mapping.
*   [x] **Hardening: Structured Error Handling**: Replace nullable/boolean returns in I/O and Media services with Sealed Result types for better error propagation.
*   [x] **Hardening: Atomic DAO Operations**: Audit and wrap complex multi-table operations in `@Transaction` blocks.
*   [x] **Hardening: Domain Model Decoupling**: Isolate business logic from Room-specific annotations by creating dedicated Domain Models and Data Entities with clear mapping layers.
*   [x] **Hardening: Unified Repository Helpers**: Standardize exception handling and Result mapping using `safeDatabaseCall` utilities.

## Phase 4: AI & Shared Presentation
**Goal:** Implement the reactive simulation engine, shared presentation layer, and core foundations.
*   [x] **Domain: AI Simulation Engine**: Implement randomized response selection and simulated "thinking" delay logic.
*   [x] **Domain: Agent Lifecycle & Debouncing**: Implement message counting and input debouncing to manage simulation triggers.
*   [x] **Presentation: Home ViewModel**: Implement the shared view model for chat list management and swipe-to-delete logic.
*   [x] **Presentation: Chat Detail ViewModel**: Implement the shared view model for message history, auto-scroll triggers, and "New Chat" initialization (creating chat on first message).
*   [x] **Presentation: Multi-Chat Drafts**: Implement logic for saving and restoring message drafts uniquely per Chat ID using DataStore.
*   [x] **Presentation: New Chat Intent**: Define the navigation and state handling for starting fresh conversations with the AI agent.
*   [x] **Foundation: Strongly Typed IDs**: Implement value classes for `ChatId`, `MessageId`, and `ParticipantId` to ensure type safety.
*   [x] **Foundation: Unified ID Generation**: Create a centralized service for consistent ID creation across the app.
*   [x] **Foundation: Smart Formatting Utilities**: Implement localized relative timestamps and human-readable file size formatters.
*   [x] **Foundation: Modular DI**: Refactor Koin modules for persistence, infrastructure, domain, and use cases.
*   [x] **Validation: Comprehensive Unit Tests**: Establish a full test suite for the shared module covering DAOs, Repositories, Use Cases, and ViewModels.

## Phase 4.5: Advanced Infrastructure
**Goal:** Resolve cross-platform technical gaps for media and localization.
*   [x] **Infrastructure: Media Intake Bridge**: Extend `LocalAssetManager` with Android-specific logic to handle `content://` URIs and external file streams.
*   [x] **Infrastructure: Shared Localization Strategy**: Refactor `DateTimeUtils` and shared business logic to use a platform-agnostic `Strings` provider.
*   [x] **Infrastructure: Shared Design Tokens**: Define branding colors, spacing, and typography constants in `commonMain` for UI consistency.
*   [x] **Infrastructure: Shared Logging Harness**: Implement a lightweight logging interface to replace `println` with platform-native logs (Logcat/os_log).
*   [x] **Infrastructure: Media Logic Hardening**: Implement IO safety, file deduplication via hashing, and iOS path consistency in media services.
*   [x] **Infrastructure: Runtime Path Resolution**: Implement a bridge to resolve relative filenames to platform-absolute paths in the domain mapper for seamless UI loading.

## Phase 5: Android Platform (Compose)
**Goal:** Build the Android user interface using Jetpack Compose.
*   [x] **Android: Splash & Startup Routing**: Handle the first-run restore UI vs. immediate application launch.
*   [x] **Android: Home Screen UI & Interactions**: Build the chat list UI with swipe-to-delete confirmation.
*   [x] **Android: Chat Detail Message List**: Implement bubble-style message history with auto-scroll behavior.
*   [x] **Android: Chat Detail Input & Keyboard**: Implement the message input bar with keyboard inset handling.
*   [x] **Android: Chat Detail Media Rendering**: Integrate Coil for local URI and thumbnail rendering.
*   [x] **Android: Chat Detail Media Picking**: Implement Gallery and Camera picker intents integrated with the Media Processor.
*   [x] **Android: Fullscreen Image Viewer**: Implement a zoomable fullscreen image screen with pinch-to-zoom support.
*   [x] **Android: Tappable Title Editing**: Implement title editing via the top app bar (Bonus).

## Phase 6: iOS Platform (SwiftUI)
**Goal:** Build the iOS user interface using SwiftUI.
*   [x] **iOS: KMP-Swift Interop Wrappers**: Implement native Swift wrappers for reactive observation of KMP StateFlows.
*   [x] **iOS: Splash & Startup Routing**: Handle the first-run restore UI vs. immediate application launch.
*   [x] **iOS: Home Screen UI & Interactions**: Build the chat list UI with native swipe actions and styling.
*   [x] **iOS: Chat Detail Message List**: Implement the message history view with bubble styling and auto-scroll.
*   [x] **iOS: Chat Detail Input & Keyboard**: Implement the native input field with keyboard avoiding logic.
*   [x] **iOS: Media Rendering & Picking**: Implement SwiftUI image loading and native PHPicker/Camera integration.
*   [x] **iOS: Fullscreen Image Viewer**: Implement a zoomable fullscreen image view with pinch-to-zoom support.
*   [x] **iOS: Tappable Title Editing**: Implement title editing via the navigation bar (Bonus).

## Phase 7: UI Polish & User Experience
**Goal:** Elevate the app's visual quality and interaction design.
*   [x] **Polish: Fix Startup Flicker**: Stabilize the transition from Splash to Home by refining the initial bootstrap state logic.
*   [x] **UI: Smooth Splash Transition**: Add cross-fade animations when transitioning from the Splash screen to the Home screen.
*   [x] **UI: Modern "Pill" Draft Input**: Redesign the chat input area to a modern floating pill style with better focus states.
*   [x] **UI: Drafts on Home Screen**: Display active message drafts in the chat list with a distinct "Draft" indicator.
*   [x] **UI: Full Dark Mode Support**: Implement a unified Dark Mode using semantic design tokens across Android and iOS.
*   [x] **UI: Animated Typing (Android)**: Implement a 3-dot pulse animation for the agent's typing state on Android.
*   [x] **UI: Haptic Feedback**: Integrate platform-native haptics for message sending, deleting, and errors.
*   [x] **Polish: Smart Timestamps 12h/24h**: Update `DateTimeUtils` to respect system-level 12h/24h formatting settings.
*   [x] **UI: Error Harness & Toasts**: Display the `error` state from ViewModels using Snackbars (Android) and Alerts (iOS).

## Phase 8: Final Resilience & Documentation
**Goal:** Ensure stability and finalize project resources.
*   [ ] **Validation: Offline Persistence & Documentation**: Final verification of offline behavior and update project documentation.


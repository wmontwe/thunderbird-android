# Rework App Launch Plan

## Roadmap & Implementation Plan

Part of the **Android Rearchitecture and Core Maintenance** program.

Epic: **Rework App Launch**

This document defines the phased plan to

decouple the App Launch from the Message List and establish a standalone `MainActivity` that manages app state and navigation.

---

## Scope

### In Scope

- Decoupling of `MainActivity` from the Message List logic.
- Creation of a new `MainActivity` as the primary entry point and single point for navigation decisions.
- Integration of `FeatureLauncherActivity` functionality into the new `MainActivity`.
- Implementation of a unified navigation strategy using a Navigation Library.
- Migration of app-level state management to a dedicated `MainScreenViewModel`.
- Handling of all external intents (Launcher, Notifications, Widgets) in the new `MainActivity`.
- Renaming the existing `MainActivity` to `MessageListActivity`.

### Out of Scope

- Redesign of the Message List UI (beyond decoupling from the Activity).
- Migration of all app features to Compose (only the navigation shell is initially targeted).
- Changes to the backend or synchronization logic.

### Technical & Business Requirements

#### Technical Requirements

- Use MVI architecture for the `MainScreenViewModel`.
- Ensure compatibility with existing Fragment-based destinations.
- The new `MainActivity` must be the designated `LAUNCHER` activity.
- Minimal performance impact on app startup time.

#### Business Requirements

- Maintain existing deep-linking and notification entry point functionality.
- Provide a more stable and maintainable foundation for future UI improvements.

---

## Strategic Objectives

1. **Standalone Launch**: Separate the entry point of the application from the Message List logic.
2. **Centralized Navigation**: Implement a unified navigation strategy using a Navigation Library.
3. **State Management**: Move app-level state and navigation logic into a dedicated MVI-based `MainScreenViewModel`.
4. **Decoupling**: Reduce the complexity of the Message List by removing its responsibility for top-level navigation and app state.

---

## Guidelines

- `MainActivity` is the new entry point and navigation coordinator.
- `MessageListActivity` (formerly `MainActivity`) becomes a destination or a container for the Message List UI.
- All external intents (Notifications, Widgets, Launcher) should be handled by `MainActivity`.
- Navigation between major features should go through the `MainScreenViewModel` and `MainActivity`.

---

## Milestone 0: Navigation 3 Spike

**Objective: Evaluate Jetpack Navigation 3 for the Rework App Launch epic.**

- **Task 0.1: Research Navigation 3 Fundamentals**:
  - Evaluate how Navigation 3 handles State-driven navigation (MVI compatibility).
  - Analyze the new approach to "Composition-local" navigation vs traditional BackStack management.
- **Task 0.2: Interoperability Assessment**:
  - Verify how Navigation 3 supports existing Fragment-based destinations (`MessageListFragment`, `MessageViewContainerFragment`).
  - Evaluate the effort to bridge current XML-based/Legacy Activity navigation into the Navigation 3 model.
- **Task 0.3: Dependency & Stability Review**:
  - Check the current release status (Alpha/Beta) and potential for breaking changes.
  - Assess if it fits the "standalone library" goal of the Thunderbird Mobile Components.

## Milestone 1: Preparation & Renaming

**Objective: Clear the path for the new architecture by renaming existing components.**

- **Task 1.1: Rename `MainActivity` to `MessageListActivity`**:
  - Rename `com.fsck.k9.activity.MainActivity` to `MessageListActivity`.
  - Update `AndroidManifest.xml` and all internal references.
- **Task 1.2: Refactor `MessageListActivity`**:
  - Begin stripping non-message list related logic (e.g., startup logic, deep-link handling for other features) to prepare for its move to the new `MainActivity`.

## Milestone 2: MainScreen MVI Foundation

**Objective: Define the contract for the new App Launch state and logic.**

*Note: This milestone should be executed in parallel or after Milestone 3.1, as the initial `MainActivity` is a short-lived routing activity and does not require a ViewModel. The ViewModel is introduced when `MainActivity` transitions to a long-lived navigation host.*

- **Task 2.1: Define MainScreen State**:
  - Create `MainScreenState` (e.g., loading status, account list, selected navigation item).
- **Task 2.2: Define MainScreen Events and Effects**:
  - `MainScreenEvent`: UI interactions (Account selected, Search triggered).
  - `MainScreenEffect`: Navigation commands, showing snackbars.
- **Task 2.3: Create `MainScreenViewModel`**:
  - Implement logic to derive `LocalMessageSearch` into navigation arguments.
  - Manage the lifecycle of app-wide state.

## Milestone 3: Implementation of Standalone MainActivity

**Objective: Establish the new root activity and navigation host.**

- **Task 3.1: Create new `MainActivity` (Initial Phase)**:
  - Implement a new `MainActivity` as the `MAIN`/`LAUNCHER` activity.
  - Initially, it acts as a short-lived routing activity (trampoline) using `StartupRouter` and `DatabaseUpgradeInterceptor` (no ViewModel required at this stage).
- **Task 3.2: Incorporate Feature Launcher functionality (Evolution Phase)**:
  - Transition `MainActivity` from a short-lived routing activity to a long-lived navigation host.
  - Integrate `MainScreenViewModel` and `MainScreenState` to drive navigation.
  - Move logic from `FeatureLauncherActivity` (Compose `NavHost`) into `MainActivity`.
- **Task 3.3: Move Startup Logic**:
  - Move general purpose startup logic from the old `MainActivity` (now `MessageListActivity`) to the new `MainActivity`.
- **Task 3.4: Integrate Navigation Library**:
  - Set up a Navigation Host within `MainActivity` that serves as the single point for navigation decisions.
- **Task 3.5: Navigation Implementation**:
  - Implement navigation to `MessageListFragment`.
  - Implement navigation to `MessageViewContainerFragment`.
  - Implement navigation to `MessageCompose` (Activity or Fragment).

## Milestone 4: Decoupling and Delegation

**Objective: Transition responsibilities from the old structures to the new ones.**

- **Task 4.1: Decouple App State**:
  - Move account switching and global folder state from fragments to `MainScreenViewModel`.
- **Task 4.2: Intent Handling**:
  - Update `MainActivity` to parse deep links and notification intents, delegating to the `MainScreenViewModel` to trigger the appropriate `MainScreenEffect`.
- **Task 4.3: Navigation Library Migration**:
  - Replace `FragmentManager` transactions in `MessageListActivity` with Navigation Library calls in `MainActivity`.

## Milestone 5: Verification & Cleanup

**Objective: Ensure stability and remove legacy code.**

- **Task 5.1: Functional Verification**:
  - Test all entry points: Launcher, Unread Widgets, Message List Widgets, Notifications.
- **Task 5.2: Cleanup**:
  - Remove unused legacy navigation methods from `BaseActivity` or `MessageListActivity`.
  - Ensure all `LocalMessageSearch` derivations are centralized in the ViewModel.

---

## Success Criteria

- [ ] `MainActivity` is the designated `LAUNCHER` activity in `AndroidManifest.xml`.
- [ ] `MainScreenViewModel` manages the core app state and navigation logic.
- [ ] Navigation to Message List, Message View, and Compose is handled via a Navigation Library.
- [ ] `MessageListFragment` is decoupled from app-level navigation logic.
- [ ] All external intents are correctly routed through `MainActivity`.


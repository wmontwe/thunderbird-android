# Unified Account Rework Plan

This plan outlines the steps required to introduce true unified account support with global special folders (Inbox, Outbox, Drafts, Sent, Spam, Trash) in the Thunderbird for Android app.

Currently, the app supports a single configurable Unified Folder. While this is typically used as a Unified Inbox, it can include messages from any folder that the user has selected to "integrate" into this unified view.

## Goals

- Add support for a unified account view in the drawer.
- Provide dedicated unified folders for Inbox, Outbox, Drafts, Sent, Spam, and Trash.
- Ensure that selecting a unified folder displays messages from that folder type across all accounts.
- Distinguish between the existing configurable "Unified Folder" (which aggregates multiple folders) and the new type-specific unified special folders.

## Scope

### In Scope

- Support for global special folders in the unified account view.
- Infrastructure and search capabilities to support cross-account folder-type queries.
- UI and data layer updates to expose and display unified folders in the drawer.
- Consistency in behavior between the Unified Inbox and other unified special folders.

### Out of Scope

- Support for custom unified folders (user-defined across accounts).
- Unified "Archive" folder (unless specifically requested later).
- Changes to the message list UI itself (beyond display of query results).
- Performance optimizations for extremely large sets of accounts (beyond current architecture).

## Technical & Business Requirements

### Technical Requirements

- **Consistency**: Unified folders must behave predictably. The existing configurable "Unified Folder" (Integration) should likely be mapped to the "Unified Inbox" or coexist with it, while other special folders (Sent, Drafts, etc.) specifically target their respective folder types across accounts.
- **Searchability**: The `FOLDER_TYPE` search attribute must correctly map to the underlying database folder types across all accounts.
- **Architecture**: Changes must follow the established Clean Architecture patterns (Domain -> Data -> UI).
- **Testability**: All new logic in repositories and use cases must be covered by unit tests.

### Business Requirements

- **Feature Parity**: Provide a "true" unified account experience that users expect from a modern email client.
- **Discoverability**: Unified folders should be easily accessible within the Drawer UI when the Unified Account is selected.
- **Reliability**: Unified views must accurately reflect the combined state (unread/starred counts) of their constituent folders.
- **Clarity**: Users should understand the difference between the integrated "Unified Folder" and type-specific unified views.

## Proposed Changes

### 1. Domain Layer Updates

- **`UnifiedDisplayFolderType`**: Add the following types to the enum:
  - `OUTBOX`
  - `DRAFTS`
  - `SENT`
  - `SPAM`
  - `TRASH`
- **`UnifiedDisplayAccount`**: Ensure this entity is used to represent the "Unified Account" in the drawer and that it can contain multiple folders.

### 2. Search API Updates

- **`MessageSearchField`**: Add a new field `FOLDER_TYPE` to allow filtering messages by the type of folder they belong to.
  - This will enable querying for all "SENT" messages across all accounts without needing to know specific folder IDs beforehand.

### 3. Data Layer Updates

- **`UnifiedFolderRepository`**:
  - Implement methods to create `LocalMessageSearch` for each new unified folder type.
  - The `INBOX` type (or a specific `INTEGRATED` type) will continue to use the `INTEGRATE` attribute.
  - New types (SENT, DRAFTS, etc.) will use the new `MessageSearchField.FOLDER_TYPE` to target the appropriate folders across all accounts.
- **`GetDisplayFoldersForAccount`**:
  - Update this use case to return a list of all supported `UnifiedDisplayFolder`s when the requested account ID is the `UNIFIED_ACCOUNT_ID`.

### 4. UI Layer Updates

- **Drawer UI**:
  - Ensure the drawer correctly renders the additional unified folders under the Unified Account.
  - Map each `UnifiedDisplayFolderType` to its corresponding icon and localized label.
- **Message List**:
  - Update `MessageListMetadata` to support identifying the active unified folder type.
  - Update `MessageListViewModel` to handle initialization and search creation for different unified folder types.
  - Ensure the message list title and UI correctly reflect the selected unified folder.

### 5. Navigation & Integration Updates

- **`DrawerContract`**:
  - Update `Effect.OpenUnifiedFolder` to include the `UnifiedDisplayFolderType`.
- **`MainActivity` / Navigation Logic**:
  - Update the handler for `OpenUnifiedFolder` to pass the folder type to the `MessageListFragment`.

## Milestone 1: Unified Account Special Folders

This milestone is divided into several tasks to incrementally implement and verify the unified special folders.

### Task 1: Domain & Search Infrastructure

- **1.1. Extend `UnifiedDisplayFolderType`**: Add `OUTBOX`, `DRAFTS`, `SENT`, `SPAM`, and `TRASH` to the enum.
- **1.2. Add `FOLDER_TYPE` to `MessageSearchField`**: Implement the new search field in the legacy search API to allow filtering by `FolderType`.
- **1.3. Update Legacy Search Implementation**: Ensure the underlying search engine can handle the `FOLDER_TYPE` field when building database queries.

### Task 2: Data Layer Implementation

- **2.1. Update `UnifiedFolderRepository`**:
  - Implement search creation logic for each new `UnifiedDisplayFolderType`.
  - Ensure `INBOX` continues to use the `INTEGRATE` attribute for backward compatibility with the "Unified Folder" setting.
- **2.2. Update `GetDisplayFoldersForAccount`**:
  - Modify the use case to return all supported unified folders when the unified account is selected.

### Task 3: UI Layer & Navigation

- **3.1. Drawer UI Support**:
  - Update the drawer to render the full list of unified folders.
  - Update `DrawerContract.Effect.OpenUnifiedFolder` to include `UnifiedDisplayFolderType`.
  - Ensure correct icons and localized strings are used for each folder type.
- **3.2. Navigation Integration**:
  - Update navigation logic (e.g., in `MainActivity`) to handle the new `OpenUnifiedFolder(type)` effect and pass it to the message list.

### Task 4: Message List Implementation

- **4.1. Update `MessageListMetadata`**: Add support for identifying the active unified folder type.
- **4.2. Update `MessageListViewModel`**:
  - Implement logic to handle unified folder selection during initialization.
  - Ensure correct `LocalMessageSearch` is used for the selected unified folder.
- **4.3. UI Verification**:
  - Ensure correct title and folder-specific UI (like "Empty Trash" action) are shown for each unified folder.

### Task 5: Verification & Testing

- **5.1. Unit Testing**: Unit tests for new repository methods, use case logic, and ViewModel transitions.
- **5.2. Integration Testing**: Verify search query generation for each unified folder type.
- **5.3. Manual Verification**:
  - Open the navigation drawer.
  - Select the Unified Account.
  - Verify that Outbox, Drafts, Sent, Spam, and Trash folders are visible.
  - Select each unified folder and verify that the message list shows messages from all accounts for that folder type.

## Verification Plan

- **Unit Tests**: Ensure all repository and use case changes are covered by unit tests.
- **Manual Verification**:
  - Open the navigation drawer.
  - Select the Unified Account.
  - Verify that Outbox, Drafts, Sent, Spam, and Trash folders are visible (if they contain messages or are configured to be shown).
  - Select each unified folder and verify that the message list shows messages from all accounts for that folder type.


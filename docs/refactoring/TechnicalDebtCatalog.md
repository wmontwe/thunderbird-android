## Technical Debt Catalog

This document catalogs refactoring opportunities in the Thunderbird for Android codebase. It's designed to help contributors find areas to work on and understand the expected outcomes for each refactoring task.

**Note:** MainActivity/MessageActivity architectural refactoring is covered separately in [MainActivityTransitionPlan.md](MainActivityTransitionPlan.md) and is currently in progress. This catalog focuses on other refactoring opportunities throughout the codebase.

**Status Legend:**
- 🟢 **Good First Issue** - Well-scoped, clear requirements, limited scope
- 🟡 **Intermediate** - Requires deeper understanding of the codebase
- 🔴 **Advanced** - Complex, far-reaching changes requiring significant experience

---

### 1. BaseMessageListFragment: Extract Search View Management

**Status:** 🟡 Intermediate
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/messagelist/BaseMessageListFragment.kt`
**Estimated:** ~200-300 lines of search-related code

#### Problem

Search view logic is embedded in `BaseMessageListFragment`:
- Menu inflation with search action
- SearchView expansion/collapse handling
- Query text change listeners
- Search submission to Search activity
- State preservation

This makes the fragment harder to:
- Test search behavior independently
- Reuse search UI in other contexts
- Maintain as search requirements evolve

#### Expected Outcome

Extract search handling into a dedicated component (Compose-based or separate manager):

```kotlin
class MessageListSearchHandler(
    private val onSearchRequested: (query: String, account: LegacyAccount?, folderId: Long?) -> Boolean,
) {
    fun setupSearchView(menu: Menu, menuInflater: MenuInflater)
    fun expandSearchView()
    fun collapseSearchView()
    fun isCollapsed(): Boolean
}
```

Or consider migrating to a Compose-based search bar that can be shared across features.

**Benefits:**
- Testable search behavior
- Potential for Compose migration
- Reusable across message list variations
- Clearer separation of concerns

**Scope:**
- Extract SearchView setup and handling
- Handle state preservation
- Consider Compose migration path
- Add tests for search interactions

---

### 2. LocalMessageSearch: Improve API and Documentation

**Status:** 🟡 Intermediate
**Component:** Multiple files using `LocalMessageSearch`
**Locations:** `feature/search/legacy/`

#### Problem

`LocalMessageSearch` is used throughout the codebase but:
- Its API is mutation-based (add conditions, or conditions, etc.)
- Difficult to understand what a search does without tracing mutations
- Easy to create invalid search states
- No immutability guarantees
- Serialization/deserialization is opaque

#### Expected Outcome

**Option A - Documentation Pass:**
Add comprehensive KDoc explaining:
- How to build common search types
- What each method does and when to use it
- Examples of typical search patterns
- Serialization format

**Option B - API Improvement (more involved):**
Create a builder or DSL for constructing searches:

```kotlin
val search = localMessageSearch {
    account(accountUuid)
    folder(folderId)
    conditions {
        or {
            field(SENDER) contains "john"
            field(SUBJECT) contains "meeting"
        }
        and {
            field(DATE) after Date(...)
        }
    }
}
```

**Benefits:**
- Clearer search construction
- Harder to create invalid states
- Better IDE autocomplete
- Easier to test search logic

**Scope:**
- Document existing API thoroughly
- Consider builder pattern or DSL
- Add validation for search states
- Improve serialization clarity

---

### 3. MessageReference: Consider Value Class or Inline Class

**Status:** 🟢 Good First Issue
**Component:** `app.k9mail.legacy.message.controller.MessageReference`
**Usage:** Passed around extensively to identify messages

#### Problem

`MessageReference` is a data class containing:
- Account UUID (String)
- Folder ID (Long)
- Message UID (String)

It's created and passed around frequently, but it's a full object allocation.

#### Expected Outcome

Consider converting to a Kotlin value class or using a more efficient representation:

```kotlin
@JvmInline
value class MessageReference(private val encoded: String) {
    val accountUuid: String get() = // decode from string
    val folderId: Long get() = // decode from string
    val uid: String get() = // decode from string

    companion object {
        fun create(accountUuid: String, folderId: Long, uid: String): MessageReference
        fun parse(encoded: String): MessageReference?
    }
}
```

**Benefits:**
- Reduced object allocations
- Still type-safe
- Smaller memory footprint
- Faster equality checks

**Considerations:**
- May not be worth it if allocations aren't a bottleneck
- Profile before/after to measure impact
- Ensure serialization still works

**Good First Issue because:**
- Well-defined scope
- Performance optimization with clear metrics
- Good introduction to Kotlin value classes
- Easy to benchmark and validate

---

### 4. MessageCompose: Decompose God Activity

**Status:** 🔴 Advanced
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MessageCompose.java`

#### Problem

`MessageCompose` is a 2100+ line Java activity that handles multiple responsibilities:
- Identity selection and management
- Recipient handling
- Attachment management
- PGP encryption/signing
- Message building and sending
- Draft saving
- UI management (menus, dialogs, permissions)

It also uses the deprecated `AsyncTask` for background operations. This makes the class extremely difficult to test, maintain, and extend.

#### Expected Outcome

Decompose the activity into smaller, testable components and migrate to Kotlin:
- **Kotlin Migration:** Convert the class and its inner classes to Kotlin.
- **Decomposition:** Extract logic into dedicated managers or ViewModels (e.g., `AttachmentManager`, `ComposeIdentityManager`, `RecipientManager`).
- **Modern Concurrency:** Replace `AsyncTask` with Kotlin Coroutines.
- **Improved Testing:** Add unit tests for the extracted components.

**Benefits:**
- Better separation of concerns
- Improved testability
- Modern codebase (Kotlin, Coroutines)
- Reduced risk of regressions when modifying compose logic

**Scope:**
- Convert to Kotlin
- Extract attachment handling
- Extract PGP logic
- Replace `AsyncTask` with Coroutines
- Add tests for new components

---

### 5. MessagingController: Decompose and Migrate to Kotlin

**Status:** 🔴 Advanced
**Component:** `legacy/core/src/main/java/com/fsck/k9/controller/MessagingController.java`

#### Problem

`MessagingController` is a massive Java class (2800+ lines) that serves as the central hub for almost all mail operations (sync, fetch, move, delete, send, etc.).
- It's a "God Class" that mixes many different domains.
- It's difficult to unit test due to its size and many dependencies.
- It's still in Java, missing Kotlin's benefits.

#### Expected Outcome

Break down `MessagingController` into smaller, specialized controllers and migrate to Kotlin:
- **Specialization:** Create domain-specific controllers (e.g., `SyncController`, `MessageActionController`, `FolderController`).
- **Kotlin Migration:** Convert to idiomatic Kotlin.
- **Dependency Injection:** Use Koin to inject dependencies into the new controllers.
- **Testability:** Ensure each new controller has comprehensive unit tests.

**Benefits:**
- Clearer architecture
- Easier to test and mock specific mail operations
- Improved maintainability
- Path towards removing legacy `MessagingListener` in favor of more modern patterns (Flows, etc.)

**Scope:**
- Identify domain boundaries within `MessagingController`
- Incrementally extract functionality into new Kotlin-based controllers
- Update call sites to use new controllers
- Ensure no regressions in background mail operations

---

### 6. Replace AsyncTask and AsyncTaskLoader with Coroutines

**Status:** 🟡 Intermediate
**Component:** Multiple legacy components (e.g., `AttachmentController`, `RecipientLoader`, `LocalMessageLoader`)

#### Problem

Several areas of the legacy module still rely on `AsyncTask` and `AsyncTaskLoader`, which are deprecated and often lead to memory leaks or complex lifecycle management.

#### Expected Outcome

Replace all remaining `AsyncTask` and `AsyncTaskLoader` implementations with Kotlin Coroutines:
- Use `ViewModelScope` or `LifecycleScope` to ensure proper cancellation.
- Migrate from `AsyncTaskLoader` to `Flow` or `suspend` functions called from ViewModels.

**Benefits:**
- Modern, safer concurrency
- Better lifecycle integration
- Improved readability and testability

**Scope:**
- Audit all `AsyncTask` and `AsyncTaskLoader` usages
- Prioritize complex or error-prone implementations
- Migrate to Coroutines/Flow
- Remove deprecated code

---

### 7. K9 Singleton: Decompose and Use Dependency Injection

**Status:** 🔴 Advanced
**Component:** `legacy/core/src/main/java/com/fsck/k9/K9.kt`

#### Problem

The `K9` object is a large singleton that holds global mutable state, constants, and mixed responsibilities (storage, feature flags, global settings). This leads to:
- Tight coupling across the entire codebase
- Difficulty in writing isolated unit tests
- Unclear ownership of settings and configuration

#### Expected Outcome

Decompose the `K9` singleton as recommended by existing TODOs:
- **Settings Migration:** Move setting-related state to `DefaultGeneralSettingsManager` and `GeneralSettings`.
- **Constant Extraction:** Move constants to their respective domain modules.
- **Dependency Injection:** Inject configuration where needed instead of relying on the global `K9` object.

**Benefits:**
- Reduced global state
- Better test isolation
- Clearer separation of concerns
- Improved modularization

**Scope:**
- Identify state that should move to `GeneralSettingsManager`
- Incrementally move settings and update usages
- Extract constants and other non-state logic

---

### 8. Strategic Java to Kotlin Migration

**Status:** 🟡 Intermediate
**Component:** ~340 Java files in the legacy module

#### Problem

While much of the project has migrated to Kotlin, over 300 Java files remain in the legacy module. This creates a "dual-language" overhead and prevents the use of modern Kotlin features in those areas.

#### Expected Outcome

A systematic migration of remaining Java files to Kotlin:
- **Priority:** Focus on files that are frequently modified or part of active refactorings.
- **Idiomatic Kotlin:** Go beyond the auto-converter to ensure null-safety, use of properties, and other Kotlin idioms.

**Benefits:**
- More consistent codebase
- Improved safety and conciseness
- Better developer experience

**Scope:**
- Maintain a list of high-priority files for migration
- Convert and refactor Java files
- Ensure full test coverage after migration

---

## How to Contribute

1. **Choose a task** based on your experience level and interests
2. **Discuss first** - Comment on related issues or start a discussion in Matrix
3. **Create a branch** following the project's naming conventions
4. **Write tests** - All refactorings should include tests
5. **Document changes** - Update relevant docs and add code comments
6. **Submit PR** - Reference this catalog in your PR description

## Related Documents

- [MainActivityTransitionPlan.md](MainActivityTransitionPlan.md) - Broader architectural refactoring
- [Code Quality Guide](../contributing/code-quality-guide.md) - Standards and best practices
- [Development Guide](../contributing/development-guide.md) - Setup and workflow

## Status Tracking

|             Task              |     Status     | Assignee | PR |             Notes             |
|-------------------------------|----------------|----------|----|-------------------------------|
| Search View Management        | 🟡 Not Started | -        | -  | Consider Compose migration    |
| LocalMessageSearch Docs       | 🟡 Not Started | -        | -  | Documentation or API redesign |
| MessageReference Value Class  | 🟢 Not Started | -        | -  | Profile first                 |
| MessageCompose Decompose      | 🔴 Not Started | -        | -  | God Activity refactor         |
| MessagingController Decompose | 🔴 Not Started | -        | -  | Massive class refactor        |
| Replace AsyncTask             | 🟡 Not Started | -        | -  | Coroutines migration          |
| K9 Singleton Decompose        | 🔴 Not Started | -        | -  | Remove global state           |
| Strategic Kotlin Migration    | 🟡 Not Started | -        | -  | ~340 Java files remaining     |

---

**Last Updated:** 2026-01-15
**Maintainer:** @wmontwe

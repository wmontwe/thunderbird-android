## MainActivity Technical Debt Catalog

This document catalogs MainActivity-specific refactoring opportunities that should be addressed as part of the MainActivity/MessageActivity transition. These tasks are complementary to the [MainActivityTransitionPlan.md](MainActivityTransitionPlan.md).

**Note:** These refactorings can be done either:
- **During** the MessageActivity extraction (Phase 1 of MainActivityTransitionPlan)
- **After** MessageActivity is created (Phase 5 decomposition)

**Status Legend:**
- 🟢 **Good First Issue** - Well-scoped, clear requirements, limited scope
- 🟡 **Intermediate** - Requires deeper understanding of the codebase
- 🔴 **Advanced** - Complex, far-reaching changes requiring significant experience

---

### 1. MainActivity: Extract IntentDecoder

**Status:** 🟡 Intermediate
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MainActivity.kt`
**Lines:** 389-570 (primarily `decodeExtrasToLaunchData()` method)

#### Problem
The `MainActivity.decodeExtrasToLaunchData()` method is a 140-line complex method that handles:
- Shortcut intents
- Search intents with query parsing
- Message reference intents
- Serialized LocalSearch objects
- Default routing logic

This logic is deeply embedded in MainActivity, making it:
- Difficult to test in isolation
- Hard to understand the full intent contract[TechnicalDebtCatalog.md](TechnicalDebtCatalog.md)
- Challenging to maintain as new entry points are added
- Impossible to reuse if MainActivity is split (see MainActivityTransitionPlan)

#### Expected Outcome
Create a dedicated `MessageIntentDecoder` class:

```kotlin
class MessageIntentDecoder(
    private val accountManager: LegacyAccountDtoManager,
    private val defaultFolderProvider: DefaultFolderProvider,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    /**
     * Decodes an intent into launch data for the message list/view.
     *
     * @return LaunchData containing search, account, message reference, etc.
     *         Returns null if the intent should finish the activity (e.g., database upgrade)
     */
    fun decode(intent: Intent): LaunchData?

    // Helper methods for specific intent types
    private fun decodeShortcutIntent(intent: Intent): LaunchData?
    private fun decodeSearchIntent(intent: Intent): LaunchData?
    private fun decodeMessageReferenceIntent(intent: Intent): LaunchData?
    private fun decodeLocalSearchIntent(intent: Intent): LaunchData?
    private fun createDefaultLaunchData(): LaunchData
}
```

**Benefits:**
- Testable intent parsing logic (unit tests without Activity)
- Clear documentation of all supported intent formats
- Reusable across MainActivity and future MessageActivity
- Easier to add new intent types

**Scope:**
- Extract method and helpers to new class
- Add comprehensive unit tests covering all intent types
- Update MainActivity to use MessageIntentDecoder
- Document all supported intent extras and actions

**Related:**
- See `MainActivityTransitionPlan.md` Phase 5
- Will be needed when creating MessageActivity

---

### 2. MainActivity: Extract NavigationDrawerManager

**Status:** 🟡 Intermediate
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MainActivity.kt`
**Lines:** 625-665, 1428-1453 (initialization and configuration)

#### Problem
Navigation drawer logic is scattered throughout MainActivity:
- `initializeDrawer()` and `initializeFolderDrawer()` - initialization
- `configureDrawer()` - account/folder selection
- `createDrawerListener()` - drawer interaction callbacks
- `lockDrawer()` / `unlockDrawer()` - state management
- `setDrawerLockState()` - conditional locking

This creates tight coupling between:
- Activity lifecycle and drawer lifecycle
- Fragment transactions and drawer state
- Search state and drawer configuration

#### Expected Outcome
Create a `NavigationDrawerManager` that encapsulates drawer logic:

```kotlin
class NavigationDrawerManager(
    private val activity: MainActivity,
    private val generalSettingsManager: GeneralSettingsManager,
    private val logger: Logger,
) {
    private var navigationDrawer: NavigationDrawer? = null

    /**
     * Initialize the drawer based on configuration
     */
    fun initialize(isDrawerEnabled: Boolean)

    /**
     * Configure drawer for the current account and search
     */
    fun configure(account: LegacyAccountDto?, search: LocalMessageSearch?, singleFolderMode: Boolean)

    /**
     * Lock/unlock drawer based on UI state
     */
    fun updateLockState(isAdditionalFragmentDisplayed: Boolean)

    /**
     * Handle drawer open/close events
     */
    fun setDrawerListener(
        onDrawerOpened: () -> Unit,
        onDrawerClosed: () -> Unit,
    )

    fun open()
    fun close()
    fun isOpen(): Boolean
}
```

**Benefits:**
- Drawer lifecycle separate from Activity lifecycle
- Testable drawer configuration logic
- Clearer relationship between UI state and drawer state
- Easier to migrate drawer when adding Bottom Navigation

**Scope:**
- Extract drawer-related fields and methods
- Create manager with clear API
- Handle drawer interaction callbacks
- Add tests for drawer state transitions

**Related:**
- Depends on Bottom Navigation decision (Phase 4 of MainActivityTransitionPlan)
- May need to integrate with FeatureLauncherActivity later

---

### 3. MainActivity: Extract MessageInteractionHandler

**Status:** 🟢 Good First Issue
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MainActivity.kt`
**Lines:** 756-993 (hotkey handling methods)

#### Problem
The `onCustomKeyDown()` method is 150+ lines of nested conditionals handling:
- Volume keys for message navigation
- Delete key for message/list deletion
- D-pad keys for message navigation
- Character keys ('c' for compose, 'o' for sort, etc.)

This logic:
- Is difficult to document (which keys do what?)
- Can't be tested without an Activity
- Mixes keyboard and volume button handling
- Duplicates logic between message list and message view modes

#### Expected Outcome
Create a `MessageInteractionHandler` that centralizes input handling:

```kotlin
class MessageInteractionHandler(
    private val generalSettingsManager: GeneralSettingsManager,
) {
    /**
     * Handle a key event in message list mode
     * @return true if the event was handled
     */
    fun handleKeyInMessageList(
        event: KeyEvent,
        messageListFragment: BaseMessageListFragment,
    ): Boolean

    /**
     * Handle a key event in message view mode
     * @return true if the event was handled
     */
    fun handleKeyInMessageView(
        event: KeyEvent,
        messageViewFragment: MessageViewContainerFragment?,
    ): Boolean

    /**
     * Check if volume keys should be used for navigation
     */
    fun shouldUseVolumeKeysForNavigation(): Boolean

    companion object {
        // Document all supported hotkeys
        const val HOTKEY_COMPOSE = 'c'
        const val HOTKEY_SORT = 'o'
        const val HOTKEY_DELETE = 'd'
        // ... etc
    }
}
```

**Benefits:**
- Centralized documentation of all keyboard shortcuts
- Testable without Activity
- Clear separation of list vs view mode interactions
- Easier to add new hotkeys or modify existing ones

**Scope:**
- Extract `onCustomKeyDown()` logic to handler
- Extract `onDeleteHotKey()` helper
- Add documentation for all hotkeys
- Add unit tests for hotkey combinations
- Consider creating a keyboard shortcuts help screen

**Good First Issue because:**
- Self-contained logic with clear inputs/outputs
- No complex dependencies
- Easy to test
- Clear success criteria

---

### 4. MainActivity: Extract MessageActionBarManager

**Status:** 🟢 Good First Issue
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MainActivity.kt`
**Lines:** 619-623, 1026-1037, 1346-1354 (action bar management)

#### Problem
Action bar title management is scattered:
- `initializeActionBar()` - initial setup
- `setActionBarTitle()` - setting title/subtitle
- `setMessageListTitle()` - called by fragments
- `showDefaultTitleView()` - reset to default
- `showMessageTitleView()` - empty title for message view

The logic for when to show which title is implicit and hard to follow.

#### Expected Outcome
Create a `MessageActionBarManager` to centralize action bar state:

```kotlin
class MessageActionBarManager(
    private val activity: AppCompatActivity,
) {
    private val actionBar: ActionBar = activity.supportActionBar!!

    /**
     * Initialize action bar with home button enabled
     */
    fun initialize()

    /**
     * Set title for message list mode
     */
    fun setMessageListTitle(title: String, subtitle: String? = null)

    /**
     * Set title for message view mode (typically empty)
     */
    fun setMessageViewTitle()

    /**
     * Update home icon (drawer menu vs back arrow)
     */
    fun setHomeIcon(icon: Int)

    /**
     * Show/hide progress indicator
     */
    fun setProgressEnabled(enabled: Boolean)
}
```

**Benefits:**
- Clear API for action bar state
- Easier to understand when titles change
- Testable without Activity (using mock ActionBar)
- Simpler to add new action bar states

**Scope:**
- Extract action bar setup and management
- Consolidate title/subtitle logic
- Add home icon management
- Simple unit tests with mocked ActionBar

**Good First Issue because:**
- Small, focused scope
- Clear state machine (list mode vs view mode)
- No complex logic or dependencies
- Good introduction to Android action bar APIs

---

### 5. ViewSwitcher: Consider Replacing with Compose Navigation

**Status:** 🔴 Advanced
**Component:** `legacy/ui/legacy/src/main/java/com/fsck/k9/view/ViewSwitcher.kt`
**Used in:** MainActivity for switching between message list and message view

#### Problem
`ViewSwitcher` is a custom view that animates between two child views. It's used in MainActivity to switch between:
- Message list (first view)
- Message view (second view)

While it works, it:
- Uses legacy View-based animations
- Requires careful state management
- Doesn't integrate with modern navigation patterns
- Makes testing MainActivity's display mode logic harder

#### Expected Outcome
**Option A - Short-term:** Extract ViewSwitcher logic into a manager
```kotlin
class MessageDisplayManager(
    private val viewSwitcher: ViewSwitcher,
    private val messageListFragment: BaseMessageListFragment,
    private val messageViewContainer: FrameLayout,
) {
    fun showMessageList()
    fun showMessageView()
    fun getCurrentDisplayMode(): DisplayMode
}
```

**Option B - Long-term:** Migrate to Compose Navigation when MessageActivity is created
- Use Compose NavHost for message list → message view transitions
- Leverage Compose animations (AnimatedContent)
- Integrate with predictive back gestures
- Better state management with Compose

**Why Advanced:**
- Requires understanding both View and Compose navigation
- Depends on MainActivityTransitionPlan completion
- Affects core navigation flow
- Needs careful migration to avoid regressions

**Scope:**
- Analyze ViewSwitcher usage patterns
- Design Compose-based replacement
- Migrate animations to Compose transitions
- Update fragment management for Compose
- Extensive testing of navigation flows

**Related:**
- Blocked by MainActivityTransitionPlan Phase 1-3
- Part of eventual Compose migration strategy

---

### 6. Split MainActivity Layouts: Consolidate or Clarify

**Status:** 🟢 Good First Issue
**Component:**
- `legacy/ui/legacy/src/main/res/layout/message_list.xml`
- `legacy/ui/legacy/src/main/res/layout/split_message_list.xml`

#### Problem
MainActivity uses two different layouts based on split-view configuration:
- `message_list.xml` - single pane with ViewSwitcher
- `split_message_list.xml` - split view with two containers

The logic for choosing between them is in `MainActivity.onCreate()`:
```kotlin
if (useSplitView()) {
    setLayout(R.layout.split_message_list)
} else {
    setLayout(R.layout.message_list)
}
```

This makes it:
- Hard to understand the UI structure at a glance
- Difficult to maintain consistent IDs across layouts
- Challenging to add features that work in both modes

#### Expected Outcome
**Option A - Consolidate layouts:**
Use a single layout with conditional visibility or ConstraintLayout constraint sets

**Option B - Document clearly:**
Add comprehensive XML comments explaining:
- When each layout is used
- Which IDs must be consistent
- How fragments are attached to each layout

**Benefits:**
- Easier to maintain UI consistency
- Clearer relationship between layouts
- Simpler to add new UI elements
- Better for future Compose migration

**Scope:**
- Analyze differences between layouts
- Decide on consolidation vs documentation
- Update layouts with comments or merge them
- Test both split-view and single-pane modes

**Good First Issue because:**
- UI/layout focused (good for designers/frontend contributors)
- Self-contained scope
- Easy to test visually
- No complex logic

---

### 7. Fragment Transactions: Consider Navigation Component

**Status:** 🔴 Advanced
**Component:** MainActivity fragment management
**Lines:** Multiple locations with `fragmentManager.beginTransaction()`

#### Problem
MainActivity manually manages fragment transactions:
- Adding/removing MessageListFragment
- Adding/removing MessageViewContainerFragment
- Managing back stack
- Handling fragment lifecycle
- Coordinating fragment state

This is error-prone and verbose. The Navigation Component could simplify this.

#### Expected Outcome
Migrate to Jetpack Navigation Component (if staying with Fragments) or plan Compose migration.

**Why Advanced:**
- Requires understanding Navigation Component deeply
- Affects all navigation flows
- Complex back stack handling
- Depends on MessageActivity creation
- May be superseded by Compose migration

**Recommendation:**
- Wait for MainActivityTransitionPlan completion
- Evaluate Navigation Component vs Compose Navigation
- Consider as part of Phase 5 (MessageActivity decomposition)

---

## How to Contribute

1. **Choose a task** based on:
   - Your experience level (🟢, 🟡, or 🔴)
   - The current phase of MainActivityTransitionPlan
   - Task dependencies (check task descriptions for timing)

2. **Discuss first** - Comment on related GitHub issues or start a discussion in Matrix

3. **Coordinate with MainActivityTransitionPlan** - Some tasks should be done during specific phases

4. **Create a branch** following the project's naming conventions

5. **Write tests** - All refactorings must include tests

6. **Document changes** - Update relevant docs and add code comments

7. **Submit PR** - Reference this catalog and MainActivityTransitionPlan in your PR description

## Task Dependencies and Timing

```
MainActivityTransitionPlan Phase 0 (FeatureLauncherActivity as entry point)
  ↓
Phase 1 (Create MessageActivity)
  ├─→ Task #1: IntentDecoder (during or after)
  ├─→ Task #6: Layout Consolidation (during or after)
  └─→ Phase 2-3 (Complete transition)
       ↓
       Phase 5 (Decompose MessageActivity)
       ├─→ Task #2: NavigationDrawerManager (after Bottom Nav decision)
       ├─→ Task #3: MessageInteractionHandler (independent)
       ├─→ Task #4: MessageActionBarManager (independent)
       ├─→ Task #5: ViewSwitcher (long-term)
       └─→ Task #7: Fragment Transactions (long-term)
```

## Related Documents

- [MainActivityTransitionPlan.md](MainActivityTransitionPlan.md) - Architectural refactoring plan
- [TechnicalDebtCatalog.md](TechnicalDebtCatalog.md) - Other refactoring opportunities (non-MainActivity)
- [Code Quality Guide](../contributing/code-quality-guide.md) - Standards and best practices
- [Development Guide](../contributing/development-guide.md) - Setup and workflow

## Status Tracking

| Task | Status | Assignee | PR | Phase | Notes |
|------|--------|----------|----|----|-------|
| IntentDecoder | 🟡 Not Started | - | - | Phase 1 or 5 | Needed for MessageActivity |
| NavigationDrawerManager | 🟡 Not Started | - | - | Phase 5 | Wait for Bottom Nav decision |
| MessageInteractionHandler | 🟢 Not Started | - | - | Phase 5 | Good first issue, independent |
| MessageActionBarManager | 🟢 Not Started | - | - | Phase 5 | Good first issue, independent |
| ViewSwitcher Refactor | 🔴 Not Started | - | - | Long-term | Consider Compose migration |
| Layout Consolidation | 🟢 Not Started | - | - | Phase 1 or 5 | Good first issue |
| Fragment Transactions | 🔴 Not Started | - | - | Long-term | Wait for architecture decision |

---

**Last Updated:** 2026-01-15
**Maintainer:** @wmontwe
**Status:** Active - coordinate with MainActivityTransitionPlan

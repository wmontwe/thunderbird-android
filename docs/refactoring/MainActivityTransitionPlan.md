### FeatureLauncherActivity to App Entry Point Transition Plan

The goal of this refactoring is to establish `FeatureLauncherActivity` as the primary app entry point (MAIN/LAUNCHER), replacing `MainActivity` in this role. `MainActivity` will be extracted into a dedicated `MessageActivity` that handles only the legacy message list/view UI.

This approach leverages the existing Compose-based architecture of `FeatureLauncherActivity` and prepares the app for a future **Bottom Navigation** architecture, where `FeatureLauncherActivity` will serve as the host for top-level sections like **Mail** and **Calendar**.

#### 1. Current State Analysis

**FeatureLauncherActivity** (feature/launcher/):
*   Already Compose-based with Navigation
*   Handles: Onboarding, Account Setup/Edit, Settings, Funding, Debug
*   Uses deep link routing via `FeatureLauncherTarget`
*   Clean, modern architecture (~37 lines)

**MainActivity** (legacy/ui/legacy/):
*   Currently the MAIN/LAUNCHER entry point
*   Large class (~1670 lines) with mixed responsibilities:
    *   **Initial Routing:** Account checks, database upgrades, onboarding redirects
    *   **Intent Decoding:** Complex extras and deep links → `LaunchData`
    *   **UI Orchestration:** Split-view, single-pane, `ViewSwitcher`
    *   **Navigation Drawer:** `NavigationDrawer` and `DropDownDrawer` management
    *   **Fragment Management:** `BaseMessageListFragment` and `MessageViewContainerFragment`
    *   **Interaction Logic:** Action bar, search, hotkeys, back-press behavior

**Key Insight:** FeatureLauncherActivity is already architected as a launcher - it just needs to become the entry point and route to mail functionality.

#### 2. Proposed Phased Plan

##### Phase 0: Make FeatureLauncherActivity the Entry Point

Establish `FeatureLauncherActivity` as the MAIN/LAUNCHER and add routing to the mail feature.

*   **Move Intent Filters:** Transfer the MAIN/LAUNCHER intent filter from `MainActivity` to `FeatureLauncherActivity` in `AndroidManifest.xml`
*   **Add Startup Logic:** Move account checking and database upgrade logic from `MainActivity` to `FeatureLauncherActivity.onCreate()`:
    *   Check for incomplete accounts and remove them
    *   Redirect to Onboarding if no accounts are set up
    *   Handle database upgrade flow via `UpgradeDatabases.actionUpgradeDatabases()`
*   **Add Mail Target:** Create `FeatureLauncherTarget.Mail` to route to the mail feature
*   **Route to Mail:** After startup checks pass, route to mail (initially via existing `MainActivity`)
*   **Handle Edge Cases:** Support the `ACTION_MAIN` + `!isTaskRoot` scenario (bringing app to foreground)

**Result:** FeatureLauncherActivity becomes the app's entry point. MainActivity continues to work for mail, but is no longer the launcher.

##### Phase 1: Extract Legacy Message UI into MessageActivity

Create a dedicated activity for the mail feature by extracting MainActivity's message-specific logic.

**Option A - Interim Approach (Recommended):**
*   Keep MainActivity as-is temporarily
*   Update `MessageListLauncher` to continue targeting `MainActivity`
*   Use `FeatureLauncherTarget.Mail` to launch `MainActivity` directly
*   This allows Phase 0 to be validated independently before refactoring MainActivity

**Option B - Full Extraction:**
*   **Create `MessageActivity`:** Copy the entire `MainActivity` class, removing only startup logic
*   **Migrate All Logic:**
    *   `ViewSwitcher` and split-view/single-pane layout management
    *   Fragment management (`BaseMessageListFragment`, `MessageViewContainerFragment`)
    *   `OnBackStackChangedListener` and back-press behavior
    *   Navigation drawer (`DropDownDrawer`) initialization and interaction
    *   Action bar, search, hotkeys, and interaction logic
    *   Intent decoding (`decodeExtras()` and `decodeExtrasToLaunchData()`)
*   **No Startup Logic:** Remove account checking, database upgrades, onboarding redirects
*   **Resource Updates:** Layouts (message_list.xml, split_message_list.xml) stay in place - no migration needed
*   **Update Navigation:**
    *   Update `MessageListLauncher` to target `MessageActivity`
    *   Update `FeatureLauncherTarget.Mail` to launch `MessageActivity`
    *   Update widgets, shortcuts, and notification intents
*   **Intent Handling:** Ensure `onNewIntent()` works correctly for relaunches
*   **State Preservation:** Verify `savedInstanceState` handling across the activity boundary

**Result:** Message list/view functionality is isolated in MessageActivity. MainActivity is ready to be removed.

##### Phase 2: Add Mail to FeatureLauncherActivity Navigation

Integrate mail into the FeatureLauncherActivity's navigation system.

*   **Define Mail Route:** Add mail destination to `FeatureLauncherTarget`:
    ```kotlin
    data object Mail : FeatureLauncherTarget(
        deepLinkUri = "k9mail://mail".toUri(),
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP,
    )
    ```
*   **Handle Deep Links:** Ensure mail-related deep links (notifications, shortcuts, `k9mail://messages`) route through FeatureLauncherActivity
*   **Intent Filter Migration:** Move the `k9mail://messages` intent filter from MainActivity to FeatureLauncherActivity or MessageActivity
*   **Update All Entry Points:**
    *   Notification action intents → MessageActivity
    *   Widget intents → MessageActivity
    *   Shortcut intents → MessageActivity
    *   App-internal launches → MessageActivity
*   **Testing:** Verify all entry points (launcher, notifications, widgets, shortcuts, deep links) work correctly

**Result:** All app entry points flow through FeatureLauncherActivity and route correctly to MessageActivity.

##### Phase 3: Remove MainActivity

Clean up the now-redundant MainActivity.

*   **Verification:** Ensure no code references `MainActivity` (except for migration compatibility)
*   **Remove from Manifest:** Delete the `<activity android:name="com.fsck.k9.activity.MainActivity">` declaration
*   **Delete File:** Remove `MainActivity.kt`
*   **Rename (Optional):** Consider renaming `MessageActivity` → `MainActivity` if the legacy name is important
*   **Dependency Cleanup:** Update Koin modules, remove MainActivity-specific dependencies
*   **Documentation:** Update architecture docs to reflect the new structure

**Result:** Clean architecture with FeatureLauncherActivity as the sole entry point.

##### Phase 4: Prepare for Bottom Navigation

Enhance FeatureLauncherActivity to support multiple top-level sections.

*   **Add Bottom Navigation UI:** Update `FeatureLauncherApp` to include a `BottomNavigation` composable
*   **Define Top-Level Sections:**
    ```kotlin
    data object MailSection : FeatureLauncherTarget(...)
    data object CalendarSection : FeatureLauncherTarget(...)
    ```
*   **Activity vs Fragment Decision:** Determine whether sections launch separate activities or embed fragments
*   **Persistent Shell:** Make FeatureLauncherActivity a persistent shell that hosts section content
*   **State Management:** Ensure section state is preserved during tab switches
*   **Navigation Drawer Integration:** Decide if the navigation drawer stays in MessageActivity or moves to FeatureLauncherActivity

**Result:** FeatureLauncherActivity becomes a multi-section host, ready for Calendar and future features.

##### Phase 5: Decouple & Modernize MessageActivity

Break down MessageActivity's remaining complexity through targeted extractions.

*   **Extract IntentDecoder:** Create a dedicated class for parsing message list intents and deep links
*   **Extract NavigationDrawerManager:** Isolate drawer initialization, configuration, and interaction handling
*   **Extract InteractionHandler:** Separate hotkey handling, volume key navigation, and action mode logic
*   **Extract ActionBarManager:** Isolate action bar title, subtitle, and icon management
*   **Consider Compose Migration:** Evaluate migrating MessageActivity to Compose when feasible
*   **Fragment Modernization:** Consider replacing fragments with Compose screens in the long term

**Result:** MessageActivity is decomposed into manageable, testable components.

#### 3. Cross-Cutting Concerns

**Backstack Management:**
*   Phase 0-2: FeatureLauncherActivity launches MessageActivity as a separate task or finishes itself
*   Phase 4+: FeatureLauncherActivity becomes a persistent shell with Bottom Nav hosting sections
*   Back press behavior must be tested across all navigation paths

**Deep Link Routing:**
*   All deep links must flow through FeatureLauncherActivity's navigation system
*   Mail-specific deep links (notifications, shortcuts) route to MessageActivity
*   Compose navigation handles feature-level deep links (Onboarding, Settings, etc.)

**Task/Launch Mode Configuration:**
*   `FeatureLauncherActivity`: `singleTop` or `singleTask` as MAIN/LAUNCHER
*   `MessageActivity`: `singleTop` to match current MainActivity behavior
*   Ensure `FLAG_ACTIVITY_CLEAR_TOP` and `FLAG_ACTIVITY_SINGLE_TOP` work correctly

**State Management:**
*   Define explicit data contracts for passing state between activities
*   Create sealed classes/data classes for navigation arguments (e.g., `MailLaunchData`)
*   Preserve `savedInstanceState` within MessageActivity
*   Handle process death/recreation gracefully

**Theme Consistency:**
*   FeatureLauncherActivity uses Compose Material3 theme (already implemented)
*   MessageActivity uses legacy XML theme (AppCompat/Material2)
*   Ensure consistent status bar, navigation bar, and action bar styling
*   Use shared color tokens where possible

**Testing Strategy:**
*   Phase 0: Test launcher entry, account setup flow, database upgrades
*   Phase 1: Test all mail entry points (launcher, notifications, widgets, shortcuts)
*   Phase 2: Verify deep link routing, intent filter handling
*   Phase 3: Regression testing for all user flows
*   Phase 4+: Test Bottom Nav state preservation and section switching

**Entry Point Updates (Critical):**
*   Notification intents (`K9NotificationActionCreator`, etc.)
*   Widget intents (`MessageListWidgetProvider`, etc.)
*   Shortcut intents (`MainActivity.shortcutIntent*()` methods)
*   Internal launches (`MessageListLauncher`, `MessageActions`, etc.)
*   Deep link handlers (`k9mail://messages`)

#### 4. Proposed Final Structure

**After Phase 3:**
*   **`FeatureLauncherActivity`**: The app's `MAIN`/`LAUNCHER` entry point
    *   Handles: App startup, account checks, database upgrades
    *   Routes to: Onboarding, Settings, Funding, Mail (via MessageActivity)
    *   Architecture: Compose with Jetpack Navigation
    *   Size: ~50-100 lines
*   **`MessageActivity`**: Dedicated mail feature activity
    *   Handles: Message list/view, split-view, fragments, drawer
    *   Architecture: Legacy Fragment-based UI
    *   Size: ~1600 lines (same as current MainActivity minus startup logic)
*   **Feature Modules**: Launched or hosted by FeatureLauncherActivity

**After Phase 4 (Bottom Nav):**
*   **`FeatureLauncherActivity`**: Persistent shell with Bottom Navigation
    *   Hosts: Mail, Calendar, and future top-level sections
    *   Either embeds fragments or launches activities per section
    *   Manages: Top-level navigation, shared state, themes
*   **`MessageActivity`**: Mail section implementation
    *   Potentially embedded in FeatureLauncherActivity or launched separately
    *   Architecture decision needed: Activity vs Fragment-based integration

**After Phase 5 (Decomposed):**
*   **`MessageActivity`**: Lean orchestrator (~500-800 lines)
*   **`MessageIntentDecoder`**: Intent parsing and deep link handling
*   **`NavigationDrawerManager`**: Drawer lifecycle and interaction
*   **`MessageInteractionHandler`**: Hotkeys and input handling
*   **`MessageActionBarManager`**: Action bar state management

#### 5. Migration Considerations

**Rollback Strategy:**
*   Each phase should be feature-flagged if possible
*   Keep old MainActivity code until Phase 3 is fully validated
*   Monitor crash reports and user feedback closely
*   Have a quick rollback plan for each phase

**Compatibility:**
*   Maintain existing intent contracts during migration
*   Support legacy deep links for external integrations
*   Preserve user-visible behavior (navigation, back button, etc.)
*   Test on multiple Android versions and device configurations

**Performance:**
*   Monitor cold start time (moving launcher intent adds a hop)
*   Measure memory usage with two activities vs one
*   Profile navigation transitions for smoothness
*   Optimize intent passing and data serialization

**Accessibility:**
*   Ensure screen readers handle activity transitions
*   Verify focus management across activities
*   Test with TalkBack enabled
*   Maintain content descriptions and labels

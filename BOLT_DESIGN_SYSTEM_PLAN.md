# Bolt Design System

## Roadmap & Implementation Plan

Part of the **Android Rearchitecture and Core Maintenance** program.

Epic: **Bolt Design System**

This document defines the phased plan to transition Thunderbird Android from Material 3 to the **Bolt Design System**,
while transforming the theme and UI foundations into a standalone, versioned library hosted at:
[thunderbird-mobile-components](https://github.com/thunderbird/thunderbird-mobile-components)

Bolt is documented and evolving in its early stages here: [Bolt Design System]https://bolt.thunderbird.net/

This roadmap assumes Bolt concepts, tokens, and components will continue to mature and explicitly allows for iteration
and controlled breaking changes during early development.

---

## Strategic Objectives

1. **Brand Identity**: Establish a distinct Thunderbird Bolt visual language, reducing reliance on generic Material 3 patterns.
2. **Externalization**: Host the design system as a standalone library for use across all Thunderbird mobile initiatives.
3. **Modernization**: Implement a Compose-first, type-safe theme system with embedded fonts.
4. **Decoupling**: Minimize direct dependencies on Material 3 components over time.

---

## Guidelines

- Bolt is **UI infrastructure**, not app logic
- No app‑specific dependencies inside the design system
- Temporary Material 3 adapters are allowed, but should be minimized over time
- Once a Bolt component is ready, all app usage of the equivalent M3 component should be replaced

---

## Milestone 1: Foundation Extraction

(Short-term: 1-2 Sprints)

**Objective: Isolate the current UI logic from the main app into a library structure.

- **Task 1.1: Library shaped module**
  - Create a new standalone Gradle module `library:designsystem` (acting as the incubator for the external repo).
  - Move existing theme and UI foundation code from `core:ui` to this module.
  - Remove dependencies on other app-modules.
- **Task 1.2: Theme Migration**:
  - Migrate foundational classes (`ThemeColorScheme`, `ThemeTypography`, `ThemeShapes`) to the new module.
  - Migrate `ThemeProvider` to the new module.
- **Task 1.3 Foundation Migration**:
  - Migrate design system tokens to the new module.
- **Task 1.4 App Adaptation**:
  - Update all app modules to consume `library:designsystem`.

## Milestone 2: Bolt Branding & Foundations (Medium-term: 2-3 Sprints)

*Objective: Introduce the visual language of Bolt.*

- **Task 2.1: Embedded Fonts**:
  - Add Bolt brand fonts (e.g., Inter, Metropolis) to `res/font` in the library.
  - Implement `BoltFontFamily` and update `ThemeTypography` to use them.
- **Task 2.2: Bolt Color Palette**:
  - Define the Bolt semantic color palette (Brand, Neutral, Success, Warning, Error).
  - Implement `BoltColorScheme` to replace the M3-mapped `ThemeColorScheme`.
- **Task 2.3: Bolt Theme Provider**:
  - Create `BoltTheme` Composable.
  - Use `CompositionLocalProvider` to expose Bolt foundations.
  - *Note: Keep a thin M3 wrapper for backwards compatibility during transition.*

## Milestone 3: Component Migration - Phase A: Atoms (Medium-term: 3-4 Sprints)

*Objective: Replace the most frequently used UI elements.*

- **Task 3.1: Bolt Buttons**: Implement `BoltButton` (Filled, Outlined, Text) with Bolt-specific animations and states.
- **Task 3.2: Bolt Text**: Create a type-safe `BoltText` component that enforces the new typography scale.
- **Task 3.3: Bolt Icons**: Migrate to a Bolt-specific icon management system, prioritizing brand icons.
- **Task 3.4**: Replace M3 Button/Text usage in high-traffic screens (e.g., Message List, Login).

## Milestone 4: External Repository Transition (Long-term: Milestone)

*Objective: Decouple from the main repository.*

- **Task 4.1**: Initialize [thunderbird-mobile-components](https://github.com/thunderbird/thunderbird-mobile-components).
- **Task 4.2**: Extract the `library:designsystem` module code to the new repository.
- **Task 4.3: CI/CD Pipeline**:
  - Set up GitHub Actions for automated builds and screenshots tests.
  - Configure Maven publishing (GitHub Packages or MavenCentral).
- **Task 4.4**: Switch the main app to consume the design system via a versioned Gradle dependency.

## Milestone 5: Component Migration - Phase B: Molecules & Organisms (Long-term: Ongoing)

*Objective: Bolt-ify complex UI patterns.*

- **Task 5.1: Bolt Input Fields**: Build `BoltTextField` with custom validation states and Bolt styling.
- **Task 5.2: Bolt App Bars**: Implement `BoltTopAppBar` to replace M3 AppBars.
- **Task 5.3: Bolt Dialogs & Bottom Sheets**: Create Bolt-specific modal systems.
- **Task 5.4**: Continuous refactoring of `app-thunderbird` to remove remaining `androidx.compose.material3` imports.

---

## Success Criteria

- [ ] `thunderbird-mobile-components` repository is the single source of truth for UI.
- [ ] No `FontFamily.SansSerif` usage; all typography uses embedded Bolt fonts.
- [ ] `BoltTheme` (Compose) is the primary theme provider in `app-thunderbird`.
- [ ] Documentation (Catalog app) exists for all Bolt components in the external repo.


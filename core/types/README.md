# Core Types Module

The `core:types` module provides common, platform-agnostic data types and enums used across the Thunderbird
application. It aims to provide type-safe representations of standard values to ensure consistency and reduce errors
related to string-based identifiers.

## Key Components

### `MimeType`

An enum class representing common MIME types used in the application.

- **Supported Types:**
  - `JPEG` (includes `image/jpeg`, `image/jpg`)
  - `PNG` (includes `image/png`)
  - `PDF` (includes `application/pdf`)
  - `UNKNOWN` (fallback for unknown types)
- **Key Features:**
  - **Alias Support:** Each `MimeType` can have multiple associated content type strings (e.g., `MimeType.JPEG` supports both `image/jpeg` and `image/jpg`).
  - **Efficient Lookup:** Provides a high-performance `fromValue(String?)` lookup using a pre-computed map (O(1) average time complexity).
  - **Case Insensitivity:** Lookup is case-insensitive.

## Usage Examples

### Resolving a `MimeType` from a String

```kotlin
val contentType = "image/jpg"
val mimeType = MimeType.fromValue(contentType) // Returns MimeType.JPEG
```

### Getting the Preferred Content Type String

```kotlin
val mimeType = MimeType.PNG
val value = mimeType.value // Returns "image/png"
```

### Accessing All Associated Values

```kotlin
val values = MimeType.JPEG.values // Returns ["image/jpeg", "image/jpg"]
```

## Adding New Types

This module is intended for low-level, primitive-like types that are used by multiple modules in the project. When
adding new types, ensure they are platform-agnostic and provide necessary utility functions (like lookups or
validations) where appropriate.

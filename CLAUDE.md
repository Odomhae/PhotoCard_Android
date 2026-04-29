# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.odom.photocard.ExampleUnitTest"

# Clean build
./gradlew clean
```

## Architecture Overview

Single-activity app using **Jetpack Compose** with **MVVM** architecture and **Navigation Compose**.

### Navigation Flow

`PhotoCardApp.kt` defines three screens with a sealed `Screen` class and `ImageSourceType` enum that controls routing:

- `ImageSource` → user picks a source
  - Camera/Gallery → `ImageEdit` (crop/rotate) → `Edit`
  - Sample image or Solid color → `Edit` directly (skips `ImageEdit`)
- Navigating back from `Edit` calls `viewModel.clearState()` and pops to `ImageSource`

### State Management

`PhotoCardViewModel` holds all app state in a single `PhotoCardState` data class via `StateFlow`. Key fields:
- `bitmap: Bitmap?` — the working bitmap (loaded from URI, downloaded URL, or drawn solid color)
- `imageUri: Uri?` — only set for camera/gallery images; `bitmap` is the canonical image for rendering
- `textOverlays: List<TextOverlay>` — draggable/scalable/rotatable text items
- `selectedTextId: String?` — which overlay the `TextEditorPanel` controls

All ViewModel functions return `Unit` and mutate `_state` by copying; no side effects except the coroutine-based bitmap loaders.

### Screen Responsibilities

| Screen | File | Purpose |
|---|---|---|
| `ImageSourceScreen` | `ui/screens/ImageSourceScreen.kt` | Pick source: camera, gallery, random picsum.photos image, or solid color |
| `ImageEditScreen` | `ui/screens/ImageEditScreen.kt` | Rotate/crop a camera or gallery image before adding text |
| `EditScreen` | `ui/screens/EditScreen.kt` | Add/drag/style text overlays; share the composed image |

### Image Rendering Pipeline

The final shareable image is composed entirely in `createFinalBitmap()` (`EditScreen.kt`) at a fixed 1080×1080 px using `android.graphics.Canvas`. Text coordinates from the UI (dp offsets) are scaled by `3×` to map to the bitmap pixel space.

The `DraggableText` composable uses `detectTransformGestures` to handle single-finger drag (pan), pinch-to-zoom (font size), and two-finger rotation simultaneously.

### FileProvider

Camera photos and shared output images are both routed through `FileProvider` (`${applicationId}.provider`). Paths are defined in `res/xml/file_paths.xml`. Camera temp files go to `cacheDir`; shared output files also go to `cacheDir`.

## Key Dependencies

- **Coil 3** (`coil3`) — async image loading; used in `ImageEditScreen` to decode a URI into a software `Bitmap` (`.allowHardware(false)` is required for Canvas operations)
- **CameraX 1.4.x** — declared but image capture is handled via `ActivityResultContracts.TakePicture`, not CameraX preview
- **Navigation Compose 2.8.x** — type-unsafe string routes (not the newer type-safe API)
- **Material Icons Extended** — used for various action icons across screens

## Permissions

- `CAMERA` — runtime-requested before launching the camera intent
- `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤ 32) — runtime-requested before gallery picker
- `INTERNET` — for loading random images from `picsum.photos`

# TraceGlass — Architecture Reference

> Generated: 2026-02-21 | Source: Deep scan of all modules

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        app (shell)                              │
│  MainActivity → NavHost (7 routes) ← TraceGlassApp (6 Koin)    │
└────────┬──────────────────┬──────────────────┬──────────────────┘
         │                  │                  │
┌────────▼────────┐ ┌──────▼───────┐ ┌────────▼────────┐
│ feature:tracing │ │ feature:     │ │ feature:        │
│  TracingVM      │ │ onboarding   │ │ timelapse       │
│  TracingScreen  │ │ 3 ViewModels │ │ 4 impls         │
│  FrameAnalyzer  │ │ 5 Screens    │ │ (codec,export)  │
└──┬──┬──┬──┬──┬──┘ └──┬──┬───────┘ └──┬──┬───────────┘
   │  │  │  │  │        │  │            │  │
┌──▼──▼──┘  │  │    ┌───▼──▼───┐    ┌───▼──▼───┐
│core:camera│  │    │core:     │    │core:     │
│CameraXMgr │  │    │session   │    │timelapse │
│Flashlight │  │    │DataStore │    │interfaces│
└───────────┘  │    └──────────┘    └──────────┘
           ┌───▼───────┐
           │core:overlay│
           │HomographyH │◄── core:cv
           │Transform   │    MarkerDetector
           │Tracking    │    OpenCV JNI
           └────────────┘
```

**Architecture Pattern**: MVVM + Clean Architecture (interface-based DI)

**Key Principles**:
- Core modules expose **interfaces**, feature modules consume them
- Single `CameraXManager` provides both `CameraManager` and `FlashlightController`
- `core:timelapse` contains only interfaces; `feature:timelapse` provides implementations
- All DI via Koin module definitions; no `@Inject` annotations

## 2. Dependency Injection Graph

```kotlin
// app/TraceGlassApp.kt — Module registration order
startKoin {
    modules(cameraModule, cvModule, sessionModule,
            onboardingModule, tracingModule, timelapseModule)
}
```

| Module | Bindings | Scope |
|--------|----------|-------|
| `cameraModule` | `CameraXManager → CameraManager + FlashlightController` | singleton |
| `cvModule` | `OpenCvMarkerDetector → MarkerDetector` | singleton |
| `sessionModule` | `DataStoreSessionRepository → SessionRepository` | singleton |
| | `DataStoreSettingsRepository → SettingsRepository` | singleton |
| `onboardingModule` | `DataStoreOnboardingRepository → OnboardingRepository` | singleton |
| | `WalkthroughAnalyzer` | singleton |
| | `OnboardingViewModel, WalkthroughViewModel, SetupGuideViewModel` | viewModel |
| `tracingModule` | `OverlayTransformCalculator` | singleton |
| | `FrameAnalyzer` | **factory** (new per request) |
| | `TimelapseOperations` | singleton |
| | `TracingViewModel` (7 deps), `SettingsViewModel` | viewModel |
| `timelapseModule` | `FileSnapshotStorage → SnapshotStorage` | singleton |
| | `MediaCodecCompiler → TimelapseCompiler` | singleton |
| | `MediaStoreVideoExporter → VideoExporter` | singleton |
| | `IntentVideoSharer → VideoSharer` | singleton |

**Notable**: `FrameAnalyzer` is a **factory** (new instance per request) because it holds mutable state (`latestResult`, `snapshotCallback`).

## 3. Data Flow — Camera Frame to Screen Render

```
Camera Sensor
    │
    ▼
CameraX ImageAnalysis (1280×720, STRATEGY_KEEP_ONLY_LATEST)
    │  runs on: analysisExecutor (single-threaded)
    ▼
FrameAnalyzer.analyze(ImageProxy)
    │  extracts Y-plane buffer + rowStride
    ▼
MarkerDetector.detect(buffer, w, h, rowStride, rotation)
    │  JNI → OpenCV ArUco DICT_4X4_50
    │  returns: MarkerResult(markers[], detectionTimeMs, frameW, frameH)
    ▼
FrameAnalyzer.latestResult (StateFlow<MarkerResult>)
    │  collected on: viewModelScope
    ▼
TracingViewModel.onMarkerResultReceived(result)
    │  1. TrackingStateManager.onMarkerResult() → TrackingStatus
    │  2. OverlayTransformCalculator.computeSmoothed() → OverlayTransform
    │  3. updateOverlayFromCombined() → renderMatrix: FloatArray(9)
    ▼
TracingScreen (Compose)
    │  drawWithContent { canvas.nativeCanvas.concat(Matrix(renderMatrix)) }
    ▼
Screen pixels
```

## 4. Overlay Transform Pipeline

How marker positions become the overlay matrix — calculator states, progressive degradation, `HomographySolver` and coordinate spaces — is in [Overlay transform pipeline](architecture/overlay-transform.md).

## 5. Persistence Architecture

### 5.1 DataStore Instances

| DataStore | File | Repository | Fields |
|-----------|------|------------|--------|
| `session_prefs` | Session state | `SessionRepository` | 16 (URI, transform, timelapse, lock, viewport) |
| `settings_prefs` | User settings | `SettingsRepository` | 4 (audio, break, interval, perspective) |
| `onboarding_prefs` | Onboarding | `OnboardingRepository` | 3 (completed, tooltip, tier) |

### 5.2 Session Save Strategy

- **Trigger**: Any state change (opacity, position, timelapse, etc.)
- **Debounce**: 500ms (prevents excessive writes during gesture sequences)
- **Context**: `NonCancellable` (survives ViewModel clearing)
- **Restore**: Dialog on cold start if `hasSavedOverlay == true`
- **Image**: Copied to internal storage (`filesDir/reference_image.*`) for URI stability

### 5.3 Schema Versioning

`DataStoreSessionRepository` includes `SCHEMA_VERSION = 1`. On incompatible version, session data is silently discarded and defaults returned.

## 6. Camera Architecture

`CameraXManager` (camera selection, lifecycle, threading) and `FrameAnalyzer` are described in [Camera and native architecture](architecture/camera-and-native.md).

## 7. Native/JNI Architecture

The OpenCV JNI library, its ArUco config, CMake hardening and ProGuard rules are described in [Camera and native architecture](architecture/camera-and-native.md).

## 8. Navigation Architecture

**Single-Activity** with Jetpack Compose `NavHost`:

```
                    ┌──────────────────────────┐
                    │     Start Destination     │
                    │  (onboarding or tracing)  │
                    └──────────┬───────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   onboarding            walkthrough              tracing
   (3-page pager)     (camera + markers)      (main screen)
        │                      │                 │    │    │
        ▼                      ▼              settings │ about
   walkthrough              tracing          setup-guide │
                                           onboarding-reopen
```

**Start destination logic**:
- First launch → `onboarding`
- Subsequent → `tracing`
- Determined by `OnboardingRepository.isOnboardingCompleted` (DataStore)
- Loading spinner shown while DataStore initializes (no `runBlocking`)

## 9. Timelapse Pipeline

```
TimelapseSession (state machine: IDLE→RECORDING→PAUSED)
    │  periodic onCapture(index) every 5s
    ▼
FrameAnalyzer.snapshotCallback (one-shot, AtomicReference)
    │  captures camera frame + overlay as JPEG
    ▼
FileSnapshotStorage.saveSnapshot(jpegBytes, index)
    │  writes to filesDir/timelapse/snapshot_NNNN.jpg
    ▼
MediaCodecCompiler.compile(files, outputFile, fps=10)
    │  H.264 encoding via MediaCodec + MediaMuxer
    │  16-aligned resolution, 4Mbps, skips corrupt frames
    ▼
MediaStoreVideoExporter.exportToGallery(videoFile, displayName)
    │  Movies/TraceGlass/ via MediaStore, IS_PENDING atomic write
    ▼
IntentVideoSharer.shareVideo(uri)
    │  ACTION_SEND, video/mp4, chooser dialog
```

## 10. Testing Architecture

Test frameworks, test doubles, test distribution and known testing patterns are in [Testing architecture](architecture/testing.md).

## 11. Build Architecture

Build variants, ABI support and CI/CD are in [Build and security](architecture/build-and-security.md).

## 12. Security Measures

The table of security measures and where each is set is in [Build and security](architecture/build-and-security.md).

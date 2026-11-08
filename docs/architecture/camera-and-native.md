# TraceGlass — Camera and Native Architecture

### 6.1 CameraXManager

**Dual-interface singleton**: Implements `CameraManager` (lifecycle) + `FlashlightController` (torch).

**Camera selection** (`buildWideAngleSelector`):
1. Enumerate back-facing logical cameras
2. Extract focal lengths via Camera2 interop
3. Select camera with **minimum focal length** (widest FOV)
4. Apply `setWidestZoom(minZoomRatio)` for ultra-wide (e.g. 0.5× on Pixel 9 Pro)

**Lifecycle**:
- `bindPreview()`: Binds preview + optional ImageAnalysis to lifecycle
- `unbind()`: Releases camera, keeps executor alive (allows rebind)
- `close()`: Shuts down executor, idempotent, prevents future binds

**Threading**:
- `analysisExecutor`: Single-threaded `ExecutorService` for frame processing
- State flows emitted on main thread
- Executor survives `unbind()`, destroyed only in `close()`

### 6.2 FrameAnalyzer

**`ImageAnalysis.Analyzer` implementation** in `feature:tracing`.

- Extracts Y-plane `ByteBuffer` + `rowStride` from `ImageProxy`
- Calls `MarkerDetector.detect()` with rotation from `imageProxy.imageInfo.rotationDegrees`
- Emits `MarkerResult` via `latestResult: StateFlow`
- **Snapshot capture**: One-shot `AtomicReference<(ByteArray) -> Unit>` callback, encodes to JPEG (quality 85) on IO dispatcher

### 7.1 OpenCV Integration

```
libtraceglass_cv.so
    ├── JNI_OnLoad: Cache class/method refs (ArrayList, Pair, Float, DetectedMarker, MarkerResult)
    ├── nativeDetect: Buffer validation → Mat(h,w,CV_8UC1,buf,rowStride) → rotate → detectMarkers → build Java objects
    └── JNI_OnUnload: Release global refs
```

**ArUco config**: `DICT_4X4_50` (50 markers, 4×4 bit grid), default `DetectorParameters`

**Security hardening** (CMake):
- `-fstack-protector-strong`, `-D_FORTIFY_SOURCE=2`
- `-Wl,-z,relro -Wl,-z,now` (GOT read-only)
- `-ffile-prefix-map` + `--build-id=none` (F-Droid reproducible builds)

**ProGuard rules**: Keep `MarkerResult`, `DetectedMarker`, `OpenCvMarkerDetector` native methods (JNI uses `FindClass` with FQN).

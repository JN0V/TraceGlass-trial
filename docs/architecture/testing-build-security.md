# TraceGlass — Testing, Build and Security

Part of the [Architecture Reference](../architecture.md).

## 10. Testing Architecture

### 10.1 Framework

| Tool | Purpose |
|------|---------|
| JUnit 5 (Jupiter) | Test runner, `@Nested`, `@BeforeEach`/`@AfterEach` |
| MockK | Mocking (used sparingly — prefer fakes) |
| kotlinx.coroutines.test | `runTest`, `TestDispatcher`, `advanceUntilIdle` |
| Compose UI Test | `createComposeRule`, UI assertion |

### 10.2 Test Doubles

| Fake | Module | Purpose |
|------|--------|---------|
| `FakeCameraManager` | core:camera | Instant bind, no real camera |
| `FakeFlashlightController` | core:camera | Configurable hasFlashlight |
| `FakeMarkerDetector` | core:cv | Returns configured results, counts calls |
| `FakeSessionRepository` | core:session | In-memory DataStore, tracks save/clear counts |

### 10.3 Test Distribution

| Module | Test Files | Notable |
|--------|-----------|---------|
| core:camera | 1 (4 tests) | Lifecycle/idempotency |
| core:cv | 1 (6 tests) | Data class validation |
| core:overlay | 4 (~80 tests) | Algorithmic correctness, corner estimation |
| core:session | 3 (23 tests) | Schema sync, persistence |
| feature:tracing | 6 (~100+ tests) | ViewModel state machine, gestures, timelapse |
| feature:onboarding | 4 (~20 tests) | Page navigation, walkthrough flow |
| feature:timelapse | 5 (~30 tests) | Storage, compilation, DI |

### 10.4 Known Testing Patterns

- **viewModelScope + runTest hang**: Infinite loops in viewModelScope prevent runTest from completing. Must cancel ViewModel or use test-injectable dispatchers.
- **State mutation trap**: When testing hidden corners in a loop, create fresh calculator per iteration to avoid cascading errors from mutated smoothedCorners.
- **android.graphics.Matrix not available**: Use `MatrixUtils` (pure Kotlin) instead of `android.graphics.Matrix` in ViewModel unit tests.

## 11. Build Architecture

### 11.1 Build Variants

| Variant | Minify | Shrink Resources | ProGuard |
|---------|--------|-------------------|----------|
| debug | No | No | No |
| release | Yes | Yes | optimize.txt + custom rules |

### 11.2 ABI Support

| ABI | Device Type |
|-----|-------------|
| arm64-v8a | Modern phones (Pixel, Samsung, OnePlus) |
| armeabi-v7a | Older 32-bit ARM devices |
| x86_64 | Emulators (Intel/AMD) |

### 11.3 CI/CD

GitHub Actions (`ci.yml`):
- Trigger: push to main, PRs
- Runner: Ubuntu latest, JDK 17 (Temurin)
- Steps: Gradle wrapper validation → cache → debug build → release build → unit tests → lint
- Caches: Gradle, OpenCV SDK (SHA256 verified)

## 12. Security Measures

| Measure | Location |
|---------|----------|
| Cleartext HTTP blocked | `network_security_config.xml` |
| Stack protector | CMake: `-fstack-protector-strong` |
| FORTIFY_SOURCE | CMake: `-D_FORTIFY_SOURCE=2` |
| RELRO + NOW | CMake: `-Wl,-z,relro -Wl,-z,now` |
| ProGuard/R8 | Release builds: minification + shrinking |
| JNI class keep | `proguard-rules.pro` in core:cv |
| No backup | `android:allowBackup="false"` |
| Buffer validation | JNI: capacity check before Mat construction |

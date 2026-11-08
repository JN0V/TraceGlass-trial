# TraceGlass — Testing Architecture

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

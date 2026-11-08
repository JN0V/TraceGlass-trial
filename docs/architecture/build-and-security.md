# TraceGlass — Build and Security

## Build

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

## Security Measures

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

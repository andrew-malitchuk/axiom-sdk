# CLAUDE.md — axiom-sdk

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Axiom SDK** — a lightweight Android-only library that exposes the device hinge angle sensor
as idiomatic Kotlin Flow and Jetpack Compose primitives, plus posture detection and adaptive
layout utilities.

Born from [Axiom Wallpaper](https://github.com/andrewmalitchuk/axiom-wallpaper-aos).
The sensor infrastructure extracted from `presentation-core-wallpaper` and
`presentation-core-ui` into a standalone, dependency-free library.

No Koin, no KSP, no KMP — vanilla Android only.

## Naming

| Item | Value |
|------|-------|
| Repository | `axiom-sdk` |
| Maven group | `dev.axiom` |
| Maven artifact | `sdk` |
| Maven coordinate | `dev.axiom:sdk:1.0.0-SNAPSHOT` |
| SDK namespace | `dev.axiom.sdk` |
| Sample app ID | `dev.axiom.sdk.sample` |

Consumer import root:
```kotlin
implementation("dev.axiom:sdk:1.0.0")
import dev.axiom.sdk.source.hinge.rememberHingeAngle
```

## Build Commands

```bash
./gradlew build                    # Full build
./gradlew :sdk:assemble            # SDK AAR only
./gradlew :sample:assembleDebug    # Sample APK
./gradlew :sdk:testDebugUnitTest   # JVM unit tests (no device needed)
./gradlew :sdk:connectedDebugAndroidTest  # Instrumented tests (device required)
./gradlew :sdk:publishToMavenLocal # Publish AAR to ~/.m2/repository
./gradlew detekt                   # Static analysis
./gradlew ktlintCheck              # Code style check
./gradlew ktlintFormat             # Auto-fix style violations
./gradlew check                    # detekt + ktlintCheck
```

Prerequisites: JDK 21, Android SDK (compileSdk 36, minSdk 26). Gradle 9.4.1, AGP 9.1.0.

Note: detekt 1.23.8 embeds Kotlin 1.9.x which does not run on JDK 25+.
Use JDK 21 for the Gradle daemon when running `detekt`:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew detekt
```
All compilation tasks and `ktlintCheck` work on any JDK.

## Architecture

Two modules only:

| Module | Role |
|--------|------|
| `:sdk` | Published Android library (`dev.axiom:sdk`) |
| `:sample` | Demo app — NOT published, validates the SDK end-to-end |

Within `:sdk`, sources are split into two top-level packages under `dev.axiom.sdk`:

```
dev.axiom.sdk/
├── core/          # internal — implementation details, not visible to consumers
│   └── hinge/
│       └── HingeSensorManager.kt    # internal Flow<Float> bridge over SensorEventListener
└── source/        # public — all consumer-facing API declarations
    ├── hinge/
    │   └── RememberHingeAngle.kt    # @Composable State<Float> wrapper
    ├── fold/
    │   ├── FoldPosture.kt           # enum + LocalFoldPosture CompositionLocal
    │   └── FoldPostureProvider.kt   # WindowInfoTracker → FoldPosture bridge
    └── window/
        ├── WindowSize.kt            # enum + LocalWindowSize + WindowSizeProvider
        └── AdaptiveLayoutSwitcher.kt # compact / expanded / tabletop slot switcher
```

`HingeSensorManager` is `internal` because it exposes raw `SensorEventListener` callbacks
directly — callers must manage the coroutine scope themselves. `rememberHingeAngle` is the
safe, lifecycle-aware public surface that wraps it through `DisposableEffect`.

## Public API

```kotlin
// dev.axiom.sdk.source.hinge
@Composable fun rememberHingeAngle(
    noiseThreshold: Float = 0.3f,
    fallbackAngle: Float = 180f,
): State<Float>

// dev.axiom.sdk.source.fold
enum class FoldPosture { Normal, Tabletop, Book }
val LocalFoldPosture: ProvidableCompositionLocal<FoldPosture>
@Composable fun FoldPostureProvider(content: @Composable () -> Unit)

// dev.axiom.sdk.source.window
enum class WindowSize { Compact, Medium, Expanded }
val LocalWindowSize: ProvidableCompositionLocal<WindowSize>
@Composable fun WindowSizeProvider(modifier: Modifier = Modifier, content: @Composable () -> Unit)
@Composable fun AdaptiveLayoutSwitcher(
    compact: @Composable () -> Unit,
    expanded: @Composable () -> Unit,
    tabletop: @Composable () -> Unit,
)
```

## Key Patterns

- **Sensor bridging**: `callbackFlow` + `awaitClose` wraps `SensorEventListener` into a cold
  `Flow<Float>`. The flow is cold — sensor registers on collection, unregisters on cancellation.
  `Sensor.TYPE_HINGE_ANGLE` is API 30+; on older/non-foldable devices `getDefaultSensor` returns
  null and the flow completes immediately, letting `rememberHingeAngle` fall back to 180f.

- **Noise gate**: A threshold (default 0.3°) filters sub-degree jitter from the hinge sensor.
  Without it, a device sitting on a flat surface continuously emits readings that differ by
  fractions of a degree, causing needless recompositions.

- **Composition locals**: `LocalFoldPosture` and `LocalWindowSize` follow the standard Compose
  ambient pattern. Providers (`FoldPostureProvider`, `WindowSizeProvider`) sit at the Activity
  root; all descendant composables read from them without explicit parameter threading.

- **`AdaptiveLayoutSwitcher` resolution order**: Tabletop wins first (explicit posture signal),
  then non-Compact window size or Book posture maps to expanded, everything else is compact.
  This order matters — do not reorder the `when` branches.

## Testing

| Scope | Location | Runner |
|-------|----------|--------|
| `WindowSize.fromWidth()` breakpoints | `sdk/src/test/` | JVM (no device) |
| `AdaptiveLayoutSwitcher` slot logic | `sdk/src/androidTest/` | Instrumented |
| `WindowSizeProvider` local values | `sdk/src/androidTest/` | Instrumented |
| `FoldPostureProvider` posture mapping | `sdk/src/androidTest/` | Instrumented |

`FoldPostureProviderTest` uses `WindowLayoutInfoPublisherRule` from `window-testing` to inject
fake `FoldingFeature` states. It requires `createAndroidComposeRule<ComponentActivity>()` because
`WindowInfoTracker.getOrCreate(context)` needs an Activity context.

Rule order in `FoldPostureProviderTest`: `WindowLayoutInfoPublisherRule` at `order = 0` (outer),
`composeTestRule` at `order = 1` (inner). Reversing this loses fold events injected before
the tracker attaches.

## Tech Stack

| Tool | Version |
|------|---------|
| Gradle | 9.4.1 |
| AGP | 9.1.0 |
| Kotlin | 2.3.0 |
| Compose BOM | 2025.12.01 |
| androidx.window | 1.4.0 |
| Detekt | 1.23.8 |
| Java target | 21 |
| Min SDK | 26 |
| Compile SDK | 36 |

## Conventions

- `sdk` module: **explicit API mode strict** — every public declaration needs an explicit
  visibility modifier and explicit return type.
- No Koin, no KSP — SDK is DI-agnostic. Consumers instantiate or wire through their own DI.
- No KMP — Android only.
- Detekt config at `configure/detekt/detekt.yml`.
- AGP 9 built-in Kotlin: do **NOT** apply `org.jetbrains.kotlin.android` — AGP handles Kotlin.
- `android.newDsl=false` in `gradle.properties` for AGP 9 DSL compatibility.
- Convention plugin IDs applied via `id("convention.xxx")`, never `alias()` — composite build
  plugins are not in the version catalog.
- Version catalog at `gradle/libs.versions.toml` is the single source for all dependency versions.
- Configuration cache enabled (`org.gradle.configuration-cache=true`).
- `convention.publish` wires `maven-publish` + GPG `signing` for `:sdk` only. The module
  declares `group` and `version`; the plugin supplies POM metadata, `singleVariant("release")`,
  Sonatype OSSRH repository URLs, and conditional signing.
- **Signing is required for release versions, optional for SNAPSHOTs.** A version ending in
  `-SNAPSHOT` routes to the snapshot repo and skips the signing requirement so local/CI
  snapshot builds succeed without a key.
- Signing key resolution: env vars `SIGNING_KEY_ID` / `SIGNING_KEY` / `SIGNING_KEY_PASSWORD`
  (CI) or `signing.keyId` / `signing.key` / `signing.password` in `~/.gradle/gradle.properties`
  (local). `signing.key` is the ASCII-armored private key block from
  `gpg --armor --export-secret-keys <KEY_ID>`. Never commit key material to the repo.
- OSSRH credentials: env vars `OSSRH_USERNAME` / `OSSRH_PASSWORD` (CI) or
  `ossrhUsername` / `ossrhPassword` in `~/.gradle/gradle.properties` (local).

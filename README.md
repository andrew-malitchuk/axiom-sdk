# Axiom SDK

## Overview

**Axiom SDK** is a lightweight Android library that exposes the device hinge angle sensor as
idiomatic Kotlin `Flow` and Jetpack Compose primitives, plus posture detection and adaptive layout
utilities for foldable devices.

Born from [Axiom Wallpaper](https://github.com/andrew-malitchuk/axiom-wallpaper-aos) — the sensor
infrastructure extracted into a standalone, dependency-free library.

`androidx.window` exposes posture as a state machine. Axiom SDK fills the gap: it packages the
continuous `Sensor.TYPE_HINGE_ANGLE` value into a Compose `State<Float>` and a Kotlin `Flow<Float>`,
and layers posture detection and adaptive layout switching on top.

No Koin, no KSP, no KMP — vanilla Android only.

## Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts (app / feature module)
dependencies {
    implementation("io.github.andrew-malitchuk:axiom-sdk:0.0.1")
}
```

## Quick Start

### Hinge angle as Compose state

```kotlin
import dev.axiom.sdk.source.hinge.rememberHingeAngle

@Composable
fun HingeDemo() {
    val angle by rememberHingeAngle()
    Text("Hinge: ${"%.1f".format(angle)}°")
}
```

### Fold posture

```kotlin
import dev.axiom.sdk.source.fold.FoldPosture
import dev.axiom.sdk.source.fold.FoldPostureProvider
import dev.axiom.sdk.source.fold.LocalFoldPosture

// At the Activity root:
FoldPostureProvider {
    // Anywhere in the composition tree:
    val posture = LocalFoldPosture.current   // Normal | Tabletop | Book
}
```

### Adaptive layout switching

```kotlin
import dev.axiom.sdk.source.window.AdaptiveLayoutSwitcher
import dev.axiom.sdk.source.window.WindowSizeProvider

WindowSizeProvider {
    AdaptiveLayoutSwitcher(
        compact  = { CompactLayout() },
        expanded = { ExpandedLayout() },
        tabletop = { TabletopLayout() },
    )
}
```

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
    compact:  @Composable () -> Unit,
    expanded: @Composable () -> Unit,
    tabletop: @Composable () -> Unit,
)
```

### `AdaptiveLayoutSwitcher` resolution order

| Condition | Slot |
|-----------|------|
| `FoldPosture.Tabletop` (any window size) | `tabletop` |
| `WindowSize.Medium`, `WindowSize.Expanded`, or `FoldPosture.Book` | `expanded` |
| Everything else (`WindowSize.Compact` + `FoldPosture.Normal`) | `compact` |

### Non-foldable / pre-API 30 devices

`Sensor.TYPE_HINGE_ANGLE` requires API 30+. On older or non-foldable devices
`getDefaultSensor()` returns null, the Flow completes immediately, and
`rememberHingeAngle` falls back to `fallbackAngle` (default 180°). Posture
stays `FoldPosture.Normal`. The SDK is safe to include in apps with `minSdk 26`.

## Sample App

The `:sample` module (`dev.axiom.sdk.sample`) demonstrates the full SDK surface on a single
screen — hinge angle gauge, live posture label, window size label, and an
`AdaptiveLayoutSwitcher` that visually highlights the active slot.

```bash
./gradlew :sample:assembleDebug
```

Install on a foldable (Pixel Fold, Galaxy Z Fold) to see live angle updates.
Install on a non-foldable phone to verify graceful fallback.

## Build Commands

```bash
./gradlew build                          # Full build
./gradlew :sdk:assemble                  # SDK AAR only
./gradlew :sample:assembleDebug          # Sample APK
./gradlew :sdk:testDebugUnitTest         # JVM unit tests (no device needed)
./gradlew :sdk:connectedDebugAndroidTest # Instrumented tests (device required)
./gradlew ktlintCheck                    # Code style check
./gradlew ktlintFormat                   # Auto-fix style violations

# detekt 1.23.8 requires JDK 21 (embedded Kotlin 1.9.x crashes on JDK 25+)
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew detekt
```

Prerequisites: JDK 21, Android SDK (compileSdk 36, minSdk 26). Gradle 9.4.1, AGP 9.1.0.

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

## Contributing

Contributions are welcome. Please follow the standard pull request process:

1. Fork the repository.
2. Create a feature branch.
3. Submit a PR with a detailed description of changes.

This project started as a personal extraction for a specific article use case. If you need
functionality that isn't currently supported, open an issue before submitting a PR.

---

Built with Jetpack Compose and androidx.window.

## License

Apache 2.0 License

```
Copyright (c) 2026 Andrew Malitchuk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

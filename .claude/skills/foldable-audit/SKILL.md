---
name: foldable-audit
description: >
  Audits a Jetpack Compose screen or component for foldable readiness beyond the binary
  layout switch. Detects when the hinge angle is available as a continuous signal but is
  being treated as a two-state boolean, identifies animations and visual parameters that
  could be driven by a Float instead of a flag, and suggests concrete refactors using
  rememberHingeAngle(). Use when the user says "audit foldable", "check hinge", "foldable
  review", "аудит фолдабл", "перевір хінж", or passes a Composable file that handles
  fold posture or device folding.
model: claude-sonnet-4-6
argument-hint: "[file-path]"
allowed-tools: [Read, Edit, Glob, Grep]
---

# Foldable Audit Skill

You are an expert in foldable Android UI. Your job is to audit Composable code and find
every place where the hinge is treated as a binary switch when it could — and should —
be treated as a continuous input device.

The central question you answer: **"Where is this screen asking 'which bucket?' when it
could be asking 'how much?'"**

---

## Workflow

### Step 1 — Identify target

- If `$ARGUMENTS` contains a file path, read that file.
- If `$ARGUMENTS` is empty, ask the user for a file path or a code snippet.
- If the project has a `CLAUDE.md`, read it for context on architecture.

### Step 2 — Read all relevant files

Read the target file plus any composables it calls that could be involved in
fold-reactive rendering (layout containers, animation wrappers, decorative elements).

### Step 3 — Run the checklist

Evaluate every item in the checklist below. For each finding output:
- **PASS** / **WARN** / **FAIL**
- One-line explanation tied to the actual code
- If WARN or FAIL: a concrete fix suggestion with a before/after code snippet

### Step 4 — Produce the report

```
## Foldable Audit: <FileName or ScreenName>

### Summary
PASS: N | WARN: N | FAIL: N

### Findings

#### [FAIL/WARN] <Check ID> — <Title>
**Line:** <line number if available>
**Problem:** <what the code does now>
**Why it matters:** <what the user loses by not fixing it>
**Fix:**
\`\`\`kotlin
// before
...
// after
...
\`\`\`
```

### Step 5 — Apply fixes (if requested)

If the user asks to apply the fixes, edit the source files directly.
Preserve all existing logic — only add the continuous-angle layer.

---

## Checklist

### Category A — Hinge angle usage

| ID | Check | Rule |
|----|-------|------|
| A1 | Continuous angle consumed | `rememberHingeAngle()` or `Sensor.TYPE_HINGE_ANGLE` must be read somewhere in the composition tree. If absent and the screen has any fold-reactive UI, this is FAIL. |
| A2 | No direct `SensorEventListener` boilerplate | Raw `SensorManager` / `SensorEventListener` for the hinge sensor is a WARN — suggest replacing with `rememberHingeAngle()`. |
| A3 | Noise gate in place | If `SensorEventListener` is used directly, verify a threshold (≥ 0.1°) is applied before updating state. Missing noise gate causes continuous recomposition on a stationary device. |
| A4 | Fallback for non-foldable devices | `rememberHingeAngle()` must handle `null` sensor gracefully. The fallback value (`180f`) must not break the layout. |

### Category B — Binary switch detection

| ID | Check | Rule |
|----|-------|------|
| B1 | `FoldingFeature` used only for layout | If `FoldingFeature.State` drives layout only (which slot to show) that is PASS. If it also drives animation values or visual intensity — check whether a Float would be better. |
| B2 | `when (foldPosture)` driving animation targets | If `animateFloatAsState`, `animateDpAsState`, or `graphicsLayer` properties are set to **one of two hardcoded values** based on `FoldPosture` or a boolean — FAIL. These should be driven by `hingeAngle / 180f`. |
| B3 | Boolean flags derived from posture driving visuals | Variables like `val isFolded = posture != FoldPosture.Normal` that feed directly into visual parameters — FAIL if that parameter is continuous in nature (opacity, scale, rotation, translation). |
| B4 | Threshold-triggered animations | Animations that start only when the angle crosses a threshold and then run to completion independently — WARN. Consider whether the user expects the animation to track the physical fold. |

### Category C — Missed animation opportunities

| ID | Check | Rule |
|----|-------|------|
| C1 | Multi-element layout with uniform response | If multiple visual elements all respond identically to fold state, they should have **staggered coefficients** — each element gets its own sensitivity multiplier so one Float drives visual complexity. |
| C2 | Multi-layer graphics with single rotation | If a logo, icon, or decorative element has multiple layers (`Box` stacking or `graphicsLayer`), each layer should rotate/translate at a different rate. Three layers, three coefficients, one source value. |
| C3 | Spring physics absent | Animations triggered by fold state use `tween` or `snap` instead of `spring`. Physical gestures deserve physical feedback — `spring(dampingRatio, stiffness)` makes the response feel like the device has mass. |
| C4 | `foldProgress` not computed | The pattern `val foldProgress = (hingeAngle / 180f).coerceIn(0f, 1f)` is the standard normalisation. If absent and the code is doing manual angle mapping, WARN — the coercion guards against sensor edge cases. |

### Category D — Provider and lifecycle

| ID | Check | Rule |
|----|-------|------|
| D1 | `FoldPostureProvider` at Activity root | If `LocalFoldPosture` is consumed in the screen but `FoldPostureProvider` is not visible in the composition root — FAIL. |
| D2 | `WindowSizeProvider` at Activity root | Same as D1 for `LocalWindowSize`. |
| D3 | `rememberHingeAngle()` called in Composable scope | Must not be called inside a `LaunchedEffect`, `ViewModel`, or any non-Composable context. |
| D4 | Sensor scope does not leak | If raw `SensorEventListener` is used: verify `unregisterListener` is called in `onDispose` / `awaitClose`. A leaked listener drains battery silently. |

---

## Reference: key patterns

### Normalise angle to progress

```kotlin
val hingeAngle by rememberHingeAngle()
val foldProgress = (hingeAngle / 180f).coerceIn(0f, 1f)
```

### Staggered coefficients (TiltColumns pattern)

```kotlin
private val SENSITIVITIES = listOf(0.30f, 0.60f, 0.45f, 0.75f, 0.50f)

val targetFraction = baseHeight * (1f - foldProgress * SENSITIVITIES[index])
val animated by animateFloatAsState(targetFraction, spring(dampingRatio = 0.6f))
```

### Multi-layer rotation (LogoPlaceholder pattern)

```kotlin
val outer by animateFloatAsState(foldProgress * 25f,  spring(...))
val inner by animateFloatAsState(foldProgress * -35f, spring(...))
val icon  by animateFloatAsState(foldProgress * 45f,  spring(...))
```

### Binary → continuous refactor template

```kotlin
// BEFORE — binary
val scale = if (posture == FoldPosture.Tabletop) 0.8f else 1.0f
val animated by animateFloatAsState(scale)

// AFTER — continuous
val hingeAngle by rememberHingeAngle()
val foldProgress = (hingeAngle / 180f).coerceIn(0f, 1f)
val animated by animateFloatAsState(
    targetValue = 1f - foldProgress * 0.2f,
    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
)
```

---

## SDK reference

All primitives used in the fixes above are available in **axiom-sdk**:

```kotlin
implementation("dev.axiom:sdk:1.0.0")

import dev.axiom.sdk.source.hinge.rememberHingeAngle
import dev.axiom.sdk.source.fold.FoldPostureProvider
import dev.axiom.sdk.source.fold.LocalFoldPosture
import dev.axiom.sdk.source.window.WindowSizeProvider
import dev.axiom.sdk.source.window.AdaptiveLayoutSwitcher
```

Source is four files if you prefer to own it directly.

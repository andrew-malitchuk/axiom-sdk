package dev.axiom.sdk.source.fold

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Describes the physical posture of a foldable device hinge.
 *
 * Used alongside [dev.axiom.sdk.source.window.WindowSize] in composables to drive
 * posture-aware layout decisions. On non-foldable devices this is always [Normal].
 *
 * - [Normal]: device is fully flat or not a foldable.
 * - [Tabletop]: horizontal fold + HALF_OPENED — like a laptop on a table.
 *   Natural split: preview on top half, controls on bottom half.
 * - [Book]: vertical fold + HALF_OPENED — like an open book.
 *   Natural split: main content on left screen, detail on right screen.
 *
 * @see LocalFoldPosture
 * @see FoldPostureProvider
 */
public enum class FoldPosture { Normal, Tabletop, Book }

/**
 * Composition local providing the current [FoldPosture] to descendant composables.
 *
 * Defaults to [FoldPosture.Normal] when no [FoldPostureProvider] is present in the
 * composition tree. On non-foldable devices this always remains [FoldPosture.Normal].
 *
 * @see FoldPosture
 * @see FoldPostureProvider
 */
public val LocalFoldPosture: ProvidableCompositionLocal<FoldPosture> =
    compositionLocalOf { FoldPosture.Normal }

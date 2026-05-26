package dev.axiom.sdk.source.window

import androidx.compose.runtime.Composable
import dev.axiom.sdk.source.fold.FoldPosture
import dev.axiom.sdk.source.fold.LocalFoldPosture

/**
 * Reads [LocalWindowSize] and [LocalFoldPosture] and renders the appropriate
 * content slot based on the current device posture and screen size.
 *
 * Resolution order:
 * 1. [FoldPosture.Tabletop] → [tabletop]
 * 2. [WindowSize.Medium] / [WindowSize.Expanded] or [FoldPosture.Book] → [expanded]
 * 3. Everything else → [compact]
 *
 * Place this inside a composable tree that already has [WindowSizeProvider] and
 * [dev.axiom.sdk.source.fold.FoldPostureProvider] in scope
 * (e.g. inside your activity's `setContent`).
 *
 * @param compact Content for phones in portrait or normal flat posture.
 * @param expanded Content for large screens, tablets, or book-fold posture.
 * @param tabletop Content for tabletop (horizontal fold) posture.
 * @see LocalWindowSize
 * @see LocalFoldPosture
 */
@Composable
public fun AdaptiveLayoutSwitcher(
    compact: @Composable () -> Unit,
    expanded: @Composable () -> Unit,
    tabletop: @Composable () -> Unit,
) {
    val windowSize: WindowSize = LocalWindowSize.current
    val foldPosture: FoldPosture = LocalFoldPosture.current

    when {
        foldPosture == FoldPosture.Tabletop -> tabletop()
        windowSize != WindowSize.Compact || foldPosture != FoldPosture.Normal -> expanded()
        else -> compact()
    }
}

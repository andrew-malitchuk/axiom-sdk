package dev.axiom.sdk.source.window

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Window Width Size Class following the canonical breakpoints.
 *
 * Used to drive adaptive layout decisions across feature screens.
 * Breakpoints align with Material 3 specification:
 * - [Compact]: width < 600dp (phones in portrait)
 * - [Medium]: 600dp <= width < 840dp (tablets in portrait, foldables)
 * - [Expanded]: width >= 840dp (tablets in landscape, desktops, panels)
 *
 * @see LocalWindowSize
 * @see WindowSizeProvider
 */
public enum class WindowSize {
    Compact,
    Medium,
    Expanded,
    ;

    public companion object {
        public val MEDIUM_BREAKPOINT: Dp = 600.dp
        public val EXPANDED_BREAKPOINT: Dp = 840.dp

        public fun fromWidth(width: Dp): WindowSize = when {
            width < MEDIUM_BREAKPOINT -> Compact
            width < EXPANDED_BREAKPOINT -> Medium
            else -> Expanded
        }
    }
}

/**
 * Composition local providing the current [WindowSize] to descendant composables.
 *
 * Defaults to [WindowSize.Compact] when no [WindowSizeProvider] is present in the
 * composition tree.
 *
 * @see WindowSize
 * @see WindowSizeProvider
 */
public val LocalWindowSize: ProvidableCompositionLocal<WindowSize> =
    compositionLocalOf { WindowSize.Compact }

/**
 * Measure the available width via [BoxWithConstraints] and provide the corresponding
 * [WindowSize] through [LocalWindowSize].
 *
 * Place this composable at the root of a screen or activity `setContent` block so that
 * all child composables can read [LocalWindowSize] without threading the value explicitly.
 *
 * @param modifier Modifier applied to the measuring [BoxWithConstraints].
 * @param content Composable tree that can read [LocalWindowSize].
 * @see LocalWindowSize
 * @see WindowSize
 */
@Composable
public fun WindowSizeProvider(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize = WindowSize.fromWidth(maxWidth)
        CompositionLocalProvider(LocalWindowSize provides windowSize) {
            content()
        }
    }
}

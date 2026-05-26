package dev.axiom.sdk.source.fold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Detect the hardware fold posture via [WindowInfoTracker] and provide the result
 * through [LocalFoldPosture].
 *
 * On non-foldable devices the tracker emits a single empty [WindowLayoutInfo][androidx.window.layout.WindowLayoutInfo]
 * and never updates, so the local stays [FoldPosture.Normal] at zero cost.
 *
 * Place this composable at the screen root (typically inside your activity's `setContent`)
 * alongside [dev.axiom.sdk.source.window.WindowSizeProvider].
 *
 * @param content Composable tree that can read [LocalFoldPosture].
 * @see LocalFoldPosture
 * @see FoldPosture
 */
@Composable
public fun FoldPostureProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var posture by remember { mutableStateOf(FoldPosture.Normal) }

    LaunchedEffect(Unit) {
        WindowInfoTracker.getOrCreate(context)
            .windowLayoutInfo(context)
            .collect { layoutInfo ->
                val foldingFeature = layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()

                posture = when {
                    foldingFeature == null -> FoldPosture.Normal
                    foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                        foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL ->
                        FoldPosture.Tabletop
                    foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                        foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL ->
                        FoldPosture.Book
                    else -> FoldPosture.Normal
                }
            }
    }

    CompositionLocalProvider(LocalFoldPosture provides posture) {
        content()
    }
}

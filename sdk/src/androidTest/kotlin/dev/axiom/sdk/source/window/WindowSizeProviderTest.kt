package dev.axiom.sdk.source.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying that [WindowSizeProvider] correctly maps container width
 * to [LocalWindowSize] values at the Material 3 breakpoints.
 *
 * Width is controlled via a [Box] with [Modifier.requiredSize] wrapped around the provider.
 * [requiredSize] overrides the incoming constraints to a fixed value regardless of the
 * actual screen dimensions, so the test result is device-independent.
 *
 * Note: [WindowSizeProvider] internally calls [androidx.compose.foundation.layout.fillMaxSize]
 * on the measuring [androidx.compose.foundation.layout.BoxWithConstraints], so the outer
 * [requiredSize] constraint propagates correctly as the max width.
 */
@RunWith(AndroidJUnit4::class)
class WindowSizeProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders [WindowSizeProvider] constrained to [widthDp] and returns the
     * [WindowSize] observed from [LocalWindowSize] inside the composition.
     *
     * Height is fixed at 800dp — it has no effect on [WindowSize] which is
     * width-only, but a non-zero height is required for the node to participate
     * in layout.
     */
    private fun observeAt(widthDp: Int): WindowSize {
        var observed: WindowSize? = null
        composeTestRule.setContent {
            Box(modifier = Modifier.requiredSize(widthDp.dp, 800.dp)) {
                WindowSizeProvider {
                    observed = LocalWindowSize.current
                }
            }
        }
        composeTestRule.runOnIdle { /* wait for composition to settle */ }
        return checkNotNull(observed)
    }

    @Test
    fun width_below_600dp_provides_compact() {
        assertEquals(WindowSize.Compact, observeAt(400))
        assertEquals(WindowSize.Compact, observeAt(599))
    }

    @Test
    fun width_at_600dp_provides_medium() {
        assertEquals(WindowSize.Medium, observeAt(600))
    }

    @Test
    fun width_between_breakpoints_provides_medium() {
        assertEquals(WindowSize.Medium, observeAt(700))
        assertEquals(WindowSize.Medium, observeAt(839))
    }

    @Test
    fun width_at_840dp_provides_expanded() {
        assertEquals(WindowSize.Expanded, observeAt(840))
    }

    @Test
    fun width_above_840dp_provides_expanded() {
        assertEquals(WindowSize.Expanded, observeAt(1280))
    }
}

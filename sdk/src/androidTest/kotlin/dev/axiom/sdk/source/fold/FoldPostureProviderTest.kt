package dev.axiom.sdk.source.fold

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying that [FoldPostureProvider] correctly translates
 * [FoldingFeature] states from [androidx.window.layout.WindowInfoTracker] into
 * [FoldPosture] values on [LocalFoldPosture].
 *
 * **Why [createAndroidComposeRule] instead of [androidx.compose.ui.test.junit4.createComposeRule]:**
 * [FoldPostureProvider] calls [androidx.window.layout.WindowInfoTracker.getOrCreate] with
 * a [android.content.Context] that must be an [android.app.Activity] — only Activities
 * have the window token required for the tracker to attach. [createAndroidComposeRule]
 * launches a real [ComponentActivity] and makes it available to the test.
 *
 * **Rule ordering:** [windowLayoutInfoPublisherRule] is declared at `order = 0` (outer) and
 * [composeTestRule] at `order = 1` (inner). JUnit applies outer rules first in `before`
 * and last in `after`, so the publisher is active for the entire Activity lifecycle.
 * Reversing the order would cause the publisher to inject events before the tracker is
 * registered, making the posture updates invisible to the composition.
 *
 * Fold states under test:
 * - No [FoldingFeature] present → [FoldPosture.Normal]
 * - [FoldingFeature.State.HALF_OPENED] + [FoldingFeature.Orientation.HORIZONTAL] → [FoldPosture.Tabletop]
 * - [FoldingFeature.State.HALF_OPENED] + [FoldingFeature.Orientation.VERTICAL] → [FoldPosture.Book]
 * - [FoldingFeature.State.FLAT] → [FoldPosture.Normal] (device fully open)
 */
@RunWith(AndroidJUnit4::class)
class FoldPostureProviderTest {

    @get:Rule(order = 0)
    val windowLayoutInfoPublisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun defaults_to_normal_when_no_folding_feature() {
        var observed = FoldPosture.Tabletop
        composeTestRule.setContent {
            FoldPostureProvider {
                observed = LocalFoldPosture.current
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(FoldPosture.Normal, observed)
        }
    }

    @Test
    fun half_opened_horizontal_fold_provides_tabletop() {
        var observed = FoldPosture.Normal
        composeTestRule.setContent {
            FoldPostureProvider {
                observed = LocalFoldPosture.current
            }
        }

        windowLayoutInfoPublisherRule.overrideWindowLayoutInfo(
            WindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = composeTestRule.activity,
                        state = FoldingFeature.State.HALF_OPENED,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )

        composeTestRule.runOnIdle {
            assertEquals(FoldPosture.Tabletop, observed)
        }
    }

    @Test
    fun half_opened_vertical_fold_provides_book() {
        var observed = FoldPosture.Normal
        composeTestRule.setContent {
            FoldPostureProvider {
                observed = LocalFoldPosture.current
            }
        }

        windowLayoutInfoPublisherRule.overrideWindowLayoutInfo(
            WindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = composeTestRule.activity,
                        state = FoldingFeature.State.HALF_OPENED,
                        orientation = FoldingFeature.Orientation.VERTICAL,
                    ),
                ),
            ),
        )

        composeTestRule.runOnIdle {
            assertEquals(FoldPosture.Book, observed)
        }
    }

    @Test
    fun flat_fold_provides_normal() {
        var observed = FoldPosture.Tabletop
        composeTestRule.setContent {
            FoldPostureProvider {
                observed = LocalFoldPosture.current
            }
        }

        windowLayoutInfoPublisherRule.overrideWindowLayoutInfo(
            WindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = composeTestRule.activity,
                        state = FoldingFeature.State.FLAT,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )

        composeTestRule.runOnIdle {
            assertEquals(FoldPosture.Normal, observed)
        }
    }
}

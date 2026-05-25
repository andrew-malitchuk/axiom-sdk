package dev.axiom.sdk.source.window

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.axiom.sdk.source.fold.FoldPosture
import dev.axiom.sdk.source.fold.LocalFoldPosture
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [AdaptiveLayoutSwitcher] slot-selection logic.
 *
 * [LocalWindowSize] and [LocalFoldPosture] are injected directly via
 * [CompositionLocalProvider] rather than wrapping the subject with real providers.
 * This keeps each test atomic — it exercises only the switching logic, not the
 * measurement or tracking code that live in [WindowSizeProvider] / [FoldPostureProvider].
 *
 * Slot selection rules under test:
 * 1. [FoldPosture.Tabletop] always wins, regardless of window size.
 * 2. [WindowSize.Medium], [WindowSize.Expanded], or [FoldPosture.Book] → expanded slot.
 * 3. [WindowSize.Compact] + [FoldPosture.Normal] → compact slot.
 *
 * Each test asserts both that the expected slot exists AND that the other two do not,
 * to catch cases where multiple slots render simultaneously.
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutSwitcherTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders [AdaptiveLayoutSwitcher] with the given [windowSize] and [foldPosture]
     * injected through composition locals. Each slot is identified by a test tag.
     */
    private fun setContent(windowSize: WindowSize, foldPosture: FoldPosture) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalWindowSize provides windowSize,
                LocalFoldPosture provides foldPosture,
            ) {
                AdaptiveLayoutSwitcher(
                    compact = { Box(Modifier.testTag("compact")) },
                    expanded = { Box(Modifier.testTag("expanded")) },
                    tabletop = { Box(Modifier.testTag("tabletop")) },
                )
            }
        }
    }

    @Test
    fun tabletop_posture_renders_tabletop_slot() {
        setContent(WindowSize.Compact, FoldPosture.Tabletop)
        composeTestRule.onNodeWithTag("tabletop").assertExists()
        composeTestRule.onNodeWithTag("compact").assertDoesNotExist()
        composeTestRule.onNodeWithTag("expanded").assertDoesNotExist()
    }

    @Test
    fun book_posture_compact_window_renders_expanded_slot() {
        setContent(WindowSize.Compact, FoldPosture.Book)
        composeTestRule.onNodeWithTag("expanded").assertExists()
        composeTestRule.onNodeWithTag("compact").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tabletop").assertDoesNotExist()
    }

    @Test
    fun medium_window_normal_posture_renders_expanded_slot() {
        setContent(WindowSize.Medium, FoldPosture.Normal)
        composeTestRule.onNodeWithTag("expanded").assertExists()
        composeTestRule.onNodeWithTag("compact").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tabletop").assertDoesNotExist()
    }

    @Test
    fun expanded_window_normal_posture_renders_expanded_slot() {
        setContent(WindowSize.Expanded, FoldPosture.Normal)
        composeTestRule.onNodeWithTag("expanded").assertExists()
        composeTestRule.onNodeWithTag("compact").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tabletop").assertDoesNotExist()
    }

    @Test
    fun compact_window_normal_posture_renders_compact_slot() {
        setContent(WindowSize.Compact, FoldPosture.Normal)
        composeTestRule.onNodeWithTag("compact").assertExists()
        composeTestRule.onNodeWithTag("expanded").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tabletop").assertDoesNotExist()
    }
}

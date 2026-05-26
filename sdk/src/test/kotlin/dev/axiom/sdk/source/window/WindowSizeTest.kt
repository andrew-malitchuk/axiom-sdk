package dev.axiom.sdk.source.window

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [WindowSize.fromWidth] breakpoint logic.
 *
 * Pure JVM — no Android runtime or emulator required. [androidx.compose.ui.unit.Dp]
 * is a Kotlin inline class with no Android dependencies, so the tests run in the
 * standard Gradle test task (`:sdk:testDebugUnitTest`).
 *
 * Breakpoints under test:
 * - < 600dp → [WindowSize.Compact]
 * - 600dp..< 840dp → [WindowSize.Medium]
 * - ≥ 840dp → [WindowSize.Expanded]
 *
 * Each boundary is tested from both sides to catch off-by-one errors.
 */
class WindowSizeTest {

    @Test
    fun `fromWidth below 600dp returns Compact`() {
        assertEquals(WindowSize.Compact, WindowSize.fromWidth(0.dp))
        assertEquals(WindowSize.Compact, WindowSize.fromWidth(599.dp))
    }

    @Test
    fun `fromWidth at 600dp boundary returns Medium`() {
        assertEquals(WindowSize.Medium, WindowSize.fromWidth(600.dp))
    }

    @Test
    fun `fromWidth between breakpoints returns Medium`() {
        assertEquals(WindowSize.Medium, WindowSize.fromWidth(601.dp))
        assertEquals(WindowSize.Medium, WindowSize.fromWidth(839.dp))
    }

    @Test
    fun `fromWidth at 840dp boundary returns Expanded`() {
        assertEquals(WindowSize.Expanded, WindowSize.fromWidth(840.dp))
    }

    @Test
    fun `fromWidth above 840dp returns Expanded`() {
        assertEquals(WindowSize.Expanded, WindowSize.fromWidth(841.dp))
        assertEquals(WindowSize.Expanded, WindowSize.fromWidth(1280.dp))
    }
}

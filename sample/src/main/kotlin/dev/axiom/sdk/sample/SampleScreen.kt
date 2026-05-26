package dev.axiom.sdk.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.axiom.sdk.source.fold.FoldPosture
import dev.axiom.sdk.source.fold.LocalFoldPosture
import dev.axiom.sdk.source.hinge.rememberHingeAngle
import dev.axiom.sdk.source.window.AdaptiveLayoutSwitcher
import dev.axiom.sdk.source.window.LocalWindowSize
import dev.axiom.sdk.source.window.WindowSize

// -- Data -----------------------------------------------------------------------------------------

private data class Feature(
    val title: String,
    val subtitle: String,
    val body: String,
    val accent: Color,
)

private val features = listOf(
    Feature(
        title = "Hinge Angle",
        subtitle = "TYPE_HINGE_ANGLE · callbackFlow",
        body = "Reports the opening angle of the foldable hinge in degrees (0–180). " +
            "Axiom bridges SensorEventListener into a cold Flow via callbackFlow + awaitClose, " +
            "so the sensor registers on collection and unregisters on cancellation automatically. " +
            "A noise gate (default 0.3°) suppresses sub-degree jitter to prevent needless " +
            "recompositions. On devices without a hinge sensor getDefaultSensor returns null " +
            "and the flow completes immediately, letting rememberHingeAngle fall back to 180°.",
        accent = Color(0xFF1565C0),
    ),
    Feature(
        title = "Fold Posture",
        subtitle = "Normal · Tabletop · Book",
        body = "Derived from Jetpack WindowInfoTracker and mapped to three semantic states. " +
            "Tabletop: device bent horizontally on a surface, screen faces up — classic laptop " +
            "posture. Book: device bent vertically in portrait, split-screen reading mode. " +
            "Normal: flat, fully open, or unknown. FoldPostureProvider sits at the Activity root " +
            "and propagates the state via LocalFoldPosture without parameter threading.",
        accent = Color(0xFF6A1B9A),
    ),
    Feature(
        title = "Window Size",
        subtitle = "Compact < 600 dp · Medium 600–840 dp · Expanded ≥ 840 dp",
        body = "WindowSizeProvider observes current window metrics on every configuration change " +
            "and classifies the available width into three breakpoints aligned with Material " +
            "Design adaptive guidelines. Compact covers phones in portrait; Medium covers phones " +
            "in landscape and small tablets; Expanded covers large tablets and desktop windows. " +
            "Any descendant reads the value from LocalWindowSize without parameter threading.",
        accent = Color(0xFF1B5E20),
    ),
    Feature(
        title = "AdaptiveLayoutSwitcher",
        subtitle = "Tabletop → Expanded → Compact",
        body = "A single composable that selects the right layout slot based on posture and window " +
            "size. Resolution order: Tabletop posture wins first as an explicit physical signal. " +
            "Then non-Compact window width or Book posture maps to the expanded slot. Everything " +
            "else falls through to compact. The order of the when branches is load-bearing — do " +
            "not reorder them. This screen itself is rendered through AdaptiveLayoutSwitcher.",
        accent = Color(0xFFBF360C),
    ),
)

// -- Screen ---------------------------------------------------------------------------------------

@Composable
internal fun SampleScreen() {
    val hingeAngle by rememberHingeAngle()
    val foldPosture = LocalFoldPosture.current
    val windowSize = LocalWindowSize.current
    var selected by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                StatusBar(
                    hingeAngle = hingeAngle,
                    foldPosture = foldPosture,
                    windowSize = windowSize,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding(),
                ) {
                    AdaptiveLayoutSwitcher(
                        compact = {
                            CompactLayout(
                                selected = selected,
                                onSelect = { selected = it },
                            )
                        },
                        expanded = {
                            ExpandedLayout(
                                selected = selected,
                                onSelect = { selected = it },
                            )
                        },
                        tabletop = {
                            TabletopLayout(
                                hingeAngle = hingeAngle,
                                selected = selected,
                                onSelect = { selected = it },
                            )
                        },
                    )
                }
            }
        }
    }
}

// -- Adaptive layouts -----------------------------------------------------------------------------

/**
 * Phone portrait: scrollable accordion list — tap a card to expand its detail inline.
 */
@Composable
private fun CompactLayout(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(features) { index, feature ->
            FeatureCard(
                feature = feature,
                selected = index == selected,
                onClick = { onSelect(index) },
            )
        }
    }
}

/**
 * Wide screen / Book posture: persistent nav rail on the left, detail panel on the right.
 */
@Composable
private fun ExpandedLayout(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .width(264.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            itemsIndexed(features) { index, feature ->
                FeatureNavItem(
                    feature = feature,
                    selected = index == selected,
                    onClick = { onSelect(index) },
                )
            }
        }
        VerticalDivider()
        FeatureDetail(
            feature = features[selected],
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
        )
    }
}

/**
 * Tabletop posture: top half shows detail (screen faces user from above), bottom half has
 * thumb-accessible chips separated by a live hinge-angle bar.
 */
@Composable
private fun TabletopLayout(
    hingeAngle: Float,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FeatureDetail(
            feature = features[selected],
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
        )
        HingeBar(hingeAngle = hingeAngle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            features.forEachIndexed { index, feature ->
                FeatureChip(
                    feature = feature,
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// -- Shared composables ---------------------------------------------------------------------------

@Composable
private fun StatusBar(
    hingeAngle: Float,
    foldPosture: FoldPosture,
    windowSize: WindowSize,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.extraSmall,
                        ),
                )
                Text(
                    text = "Axiom SDK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.3).sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusItem(label = "%.0f°".format(hingeAngle), color = Color(0xFF1565C0))
                StatusSeparator()
                StatusItem(
                    label = when (foldPosture) {
                        FoldPosture.Normal -> "Normal"
                        FoldPosture.Tabletop -> "Tabletop"
                        FoldPosture.Book -> "Book"
                    },
                    color = Color(0xFF6A1B9A),
                )
                StatusSeparator()
                StatusItem(
                    label = when (windowSize) {
                        WindowSize.Compact -> "Compact"
                        WindowSize.Medium -> "Medium"
                        WindowSize.Expanded -> "Expanded"
                    },
                    color = Color(0xFF1B5E20),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun StatusItem(label: String, color: Color) {
    Text(
        text = label,
        fontSize = 12.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(horizontal = 10.dp),
    )
}

@Composable
private fun StatusSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(14.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** Live hinge-angle bar used as the visual divider between the two halves in Tabletop mode. */
@Composable
private fun HingeBar(hingeAngle: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "HINGE",
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "%.1f°".format(hingeAngle),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        LinearProgressIndicator(
            progress = { hingeAngle / 180f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Expandable card for Compact mode — tapping selects and reveals the full body text. */
@Composable
private fun FeatureCard(
    feature: Feature,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) feature.accent.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(
                        color = if (selected) feature.accent
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = feature.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = feature.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = feature.body,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Navigation list item for the left rail in Expanded mode. */
@Composable
private fun FeatureNavItem(
    feature: Feature,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) feature.accent.copy(alpha = 0.12f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .background(
                    color = if (selected) feature.accent else Color.Transparent,
                    shape = MaterialTheme.shapes.extraSmall,
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = feature.title,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
            )
            Text(
                text = feature.subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Full detail view for the right panel (Expanded) and the top half (Tabletop). */
@Composable
private fun FeatureDetail(
    feature: Feature,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(feature.accent, MaterialTheme.shapes.extraSmall),
        )
        Text(
            text = feature.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
        )
        Text(
            text = feature.subtitle,
            fontSize = 12.sp,
            color = feature.accent,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
        HorizontalDivider()
        Text(
            text = feature.body,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Thumb-sized chip for the bottom half in Tabletop mode. */
@Composable
private fun FeatureChip(
    feature: Feature,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) feature.accent else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = feature.title,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )
    }
}

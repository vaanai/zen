package com.example.zen.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zen.data.AppUsageItem
import com.example.zen.persona.LineLibrary
import com.example.zen.persona.LocalPersona
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.components.GlassCard
import com.example.zen.ui.components.LocalHazeState
import com.example.zen.ui.components.PersonaSigil
import com.example.zen.ui.components.SectionHeader
import com.example.zen.ui.components.StatChip
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LocalPersonaColors.current
    val persona = LocalPersona.current

    val hazeState = rememberHazeState()

    // Slow, subtle drift of the gradient so the backdrop feels alive without distracting.
    val drift = rememberInfiniteTransition(label = "gradientDrift")
    val shift by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawAnimatedGradient(c.gradient, shift)
            .hazeSource(hazeState)
    ) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ZenSpacing.screenGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = ZenSpacing.xl, bottom = ZenSpacing.xxl)
            ) {
                item { HeaderRow(persona.displayName, persona.statusBadge, onOpenSettings) }

                item {
                    HeroRing(
                        saves = uiState.savesToday,
                        spentMinutes = uiState.totalTimeSpentMinutes,
                        capMinutes = uiState.dailyCapMinutes,
                        savesLabel = LineLibrary.savesLabel(persona)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ZenSpacing.sm, bottom = ZenSpacing.xl),
                        horizontalArrangement = Arrangement.spacedBy(ZenSpacing.md)
                    ) {
                        StatChip(
                            value = "${uiState.streakDays}",
                            label = "Day streak",
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            value = "${uiState.totalTimeSpentMinutes}m",
                            label = "Today / ${uiState.dailyCapMinutes}m",
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            value = "${uiState.savesTotal}",
                            label = "All-time",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { ShieldCard(persona) }

                item {
                    SectionHeader("System settings", Modifier.padding(top = ZenSpacing.sm))
                }
                item {
                    PermissionCard(
                        title = "Accessibility Blocker Service",
                        description = "Required to detect and block Reels / Shorts.",
                        isActive = uiState.isAccessibilityEnabled,
                        pulseAlpha = pulseAlpha,
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                    Spacer(Modifier.height(ZenSpacing.md))
                }
                item {
                    PermissionCard(
                        title = "Screen Time Usage Access",
                        description = "Required for screen-time stats on this dashboard.",
                        isActive = uiState.isUsageAccessEnabled,
                        pulseAlpha = pulseAlpha,
                        onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    )
                    Spacer(Modifier.height(ZenSpacing.sectionGap))
                }

                if (uiState.isUsageAccessEnabled && uiState.appStatsList.isNotEmpty()) {
                    item { SectionHeader("Detailed screen time") }
                    items(uiState.appStatsList) { app ->
                        AppStatsCard(app = app)
                        Spacer(Modifier.height(ZenSpacing.sm))
                    }
                }
            }
        }
    }
}

/** Draws the persona's vertical gradient with a slow animated vertical drift. */
private fun Modifier.drawAnimatedGradient(stops: List<Color>, shift: Float): Modifier =
    this.background(
        Brush.verticalGradient(
            colors = stops,
            startY = -400f * shift,
            endY = Float.POSITIVE_INFINITY
        )
    )

@Composable
private fun HeaderRow(name: String, badge: String, onOpenSettings: () -> Unit) {
    val c = LocalPersonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = ZenSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary
            )
            Spacer(Modifier.height(ZenSpacing.xs))
            Surface(color = c.accent.copy(alpha = 0.18f), shape = ZenRadius.pill) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent,
                    modifier = Modifier.padding(horizontal = ZenSpacing.md, vertical = ZenSpacing.xs)
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, "Settings", tint = c.textSecondary)
        }
    }
}

/** Animated hero: the ring sweep eases toward the cap and the saves number counts up. */
@Composable
private fun HeroRing(saves: Int, spentMinutes: Long, capMinutes: Int, savesLabel: String) {
    val c = LocalPersonaColors.current

    val limit = capMinutes.toFloat().coerceAtLeast(1f)
    val target = (spentMinutes / limit).coerceIn(0f, 1f)
    val overCap = spentMinutes >= capMinutes
    val sweepFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ringSweep"
    )
    val animatedSaves by animateIntAsState(saves, tween(700), label = "savesCount")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(232.dp)
            .padding(ZenSpacing.lg)
    ) {
        // Soft accent glow behind the ring.
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(c.accent.copy(alpha = 0.18f))
                .blur(40.dp)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = c.textPrimary.copy(alpha = 0.06f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            val sweep = (sweepFraction * 360f).coerceAtLeast(2f)
            drawArc(
                brush = Brush.sweepGradient(
                    if (overCap) listOf(c.danger, c.warn, c.danger)
                    else listOf(c.accentSecondary, c.accent, c.accentSecondary)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedSaves",
                style = MaterialTheme.typography.displayLarge,
                color = c.textPrimary
            )
            Text(
                text = savesLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary
            )
        }
    }
}

@Composable
private fun ShieldCard(persona: com.example.zen.persona.Persona) {
    val c = LocalPersonaColors.current
    GlassCard(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = ZenSpacing.sectionGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonaSigil()
            Spacer(Modifier.width(ZenSpacing.lg))
            Column {
                Text(
                    text = LineLibrary.shieldTitle(persona),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary
                )
                Text(
                    text = LineLibrary.shieldDescription(persona),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isActive: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val c = LocalPersonaColors.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ZenRadius.card)
            .clickable(interactionSource = source, indication = null) { onClick() },
        pressed = pressed
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Spacer(Modifier.height(ZenSpacing.xs))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
            Spacer(Modifier.width(ZenSpacing.lg))
            if (isActive) {
                Icon(Icons.Default.CheckCircle, "Active", tint = c.safe, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    Icons.Default.Warning,
                    "Pending",
                    tint = c.warn.copy(alpha = pulseAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AppStatsCard(app: AppUsageItem) {
    val c = LocalPersonaColors.current
    val appColor = remember(app.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(app.colorHex))
        } catch (e: Exception) {
            c.accent
        }
    }
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = ZenRadius.chip, contentPadding = ZenSpacing.md) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(appColor)
                    )
                    Spacer(Modifier.width(ZenSpacing.sm))
                    Text(app.appName, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                }
                Text("${app.timeSpentMinutes} mins", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            }
            Spacer(Modifier.height(ZenSpacing.sm))
            val progress = remember(app.timeSpentMinutes) {
                (app.timeSpentMinutes / 60f).coerceIn(0.02f, 1f)
            }
            LinearProgressIndicator(
                progress = { progress },
                color = appColor,
                trackColor = c.textPrimary.copy(alpha = 0.05f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

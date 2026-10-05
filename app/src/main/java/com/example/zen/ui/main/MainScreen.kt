package com.example.zen.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.components.AppRuleRows
import com.example.zen.ui.components.GlassCard
import com.example.zen.ui.components.LocalHazeState
import com.example.zen.ui.components.RuleStatement
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LocalPersonaColors.current
    val hazeState = rememberHazeState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
            .hazeSource(hazeState)
    ) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ZenSpacing.screenGutter),
                contentPadding = PaddingValues(top = ZenSpacing.xl, bottom = ZenSpacing.xxl)
            ) {
                item { HeaderRow(onOpenSettings) }
                item {
                    RuleHero(
                        friendPassEnabled = uiState.friendPassEnabled,
                        allowedScrolls = uiState.allowedScrolls,
                        tiktokGuarded = uiState.tiktokGuarded,
                        youtubeGuarded = uiState.youtubeGuarded,
                        onOpen = onOpenSettings
                    )
                }
                item {
                    AppRuleRows(
                        isGuarded = { name -> name in uiState.guardedAppNames },
                        friendPassEnabled = uiState.friendPassEnabled,
                        grouped = true
                    )
                }
                item {
                    if (uiState.isAccessibilityEnabled) {
                        Text(
                            text = "Accessibility is on.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                            modifier = Modifier.padding(top = ZenSpacing.md)
                        )
                    } else {
                        GuardOffCard()
                    }
                }
                item {
                    Captions(
                        savesToday = uiState.savesToday,
                        usageGranted = uiState.isUsageAccessEnabled,
                        minutes = uiState.totalTimeSpentMinutes
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(onOpenSettings: () -> Unit) {
    val c = LocalPersonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = ZenSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Zen",
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = c.textSecondary)
        }
    }
}

/** The rule, readable with settings still locked. Tapping it opens the lock, then the edit. */
@Composable
private fun RuleHero(
    friendPassEnabled: Boolean,
    allowedScrolls: Int,
    tiktokGuarded: Boolean,
    youtubeGuarded: Boolean,
    onOpen: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = ZenSpacing.xl)
            .clickable(
                interactionSource = source,
                indication = null,
                onClickLabel = "Edit the rule",
                onClick = onOpen
            ),
        shape = ZenRadius.hero,
        contentPadding = ZenSpacing.xl,
        pressed = pressed
    ) {
        RuleStatement(
            friendPassEnabled = friendPassEnabled,
            allowedScrolls = allowedScrolls,
            tiktokGuarded = tiktokGuarded,
            youtubeGuarded = youtubeGuarded,
            includeLimits = false,
            emphasize = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GuardOffCard() {
    val context = LocalContext.current
    val c = LocalPersonaColors.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = source,
                indication = null,
                onClickLabel = "Turn on accessibility",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            ),
        pressed = pressed
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "The guard is off",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.warn
                )
                Spacer(Modifier.height(ZenSpacing.xs))
                Text(
                    text = "Accessibility is off. Zen can't keep this rule until it's on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
            }
            Spacer(Modifier.width(ZenSpacing.lg))
            Icon(
                Icons.Default.Warning,
                contentDescription = "Off",
                tint = c.warn,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun Captions(savesToday: Int, usageGranted: Boolean, minutes: Long) {
    val c = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ZenSpacing.xl)
    ) {
        Text(
            text = stopsCaption(savesToday),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary
        )
        if (usageGranted) {
            Spacer(Modifier.height(ZenSpacing.sm))
            Text(
                text = "Time in these apps today",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary
            )
            Text(
                text = "$minutes min",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textPrimary
            )
        }
    }
}

private fun stopsCaption(count: Int): String = if (count == 1) "1 stop today" else "$count stops today"

package com.example.zen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.zen.data.GuardMode
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing

/**
 * Friends open, a few scrolls, or all short-form stops.
 * The number applies only to a feed opened with no friend pass.
 */
@Composable
fun RuleModes(
    friendPassEnabled: Boolean,
    allowedScrolls: Int,
    onChange: (friendPassEnabled: Boolean, allowedScrolls: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = GuardMode.fromPrefs(friendPassEnabled, allowedScrolls)
    var remembered by remember {
        mutableIntStateOf(
            if (allowedScrolls in GuardMode.FEW_SCROLLS_MIN..GuardMode.FEW_SCROLLS_MAX) {
                allowedScrolls
            } else {
                GuardMode.FEW_SCROLLS_DEFAULT
            }
        )
    }
    SideEffect {
        if (allowedScrolls in GuardMode.FEW_SCROLLS_MIN..GuardMode.FEW_SCROLLS_MAX) {
            remembered = allowedScrolls
        }
    }

    Column(modifier) {
        ModeRow(
            title = "Friends open",
            description = "A feed you open yourself stops.",
            highlighted = selected == GuardMode.FRIENDS_OPEN,
            onClick = {
                val rule = GuardMode.FRIENDS_OPEN.toRule()
                onChange(rule.friendPassEnabled, rule.allowedScrolls)
            }
        )
        ModeRow(
            title = "A few scrolls",
            description = "A feed you open yourself gets 1, 2, or 3 scrolls, then it stops.",
            highlighted = selected == GuardMode.A_FEW_SCROLLS,
            onClick = {
                val rule = GuardMode.A_FEW_SCROLLS.toRule(remembered)
                onChange(rule.friendPassEnabled, rule.allowedScrolls)
            }
        )
        if (selected == GuardMode.A_FEW_SCROLLS) {
            FeedScrollPicker(
                selected = allowedScrolls,
                onSelect = { scrolls ->
                    val rule = GuardMode.A_FEW_SCROLLS.toRule(scrolls)
                    onChange(rule.friendPassEnabled, rule.allowedScrolls)
                }
            )
            Spacer(Modifier.height(ZenSpacing.md))
        }
        ModeRow(
            title = "All short-form stops",
            description = "Including videos from friends.",
            highlighted = selected == GuardMode.ALL_STOPS,
            onClick = {
                val rule = GuardMode.ALL_STOPS.toRule()
                onChange(rule.friendPassEnabled, rule.allowedScrolls)
            }
        )
    }
}

@Composable
private fun ModeRow(
    title: String,
    description: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    ZenRow(
        title = title,
        description = description,
        highlighted = highlighted,
        onClick = onClick,
        modifier = Modifier.padding(bottom = ZenSpacing.md)
    )
}

@Composable
private fun FeedScrollPicker(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val c = LocalPersonaColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = ZenSpacing.xs)) {
        Text(
            text = "Scrolls on a feed you open yourself",
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
            color = c.textSecondary
        )
        Spacer(Modifier.height(ZenSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZenSpacing.sm)
        ) {
            for (n in GuardMode.FEW_SCROLLS_MIN..GuardMode.FEW_SCROLLS_MAX) {
                val on = n == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ZenRadius.chip)
                        .background(if (on) c.accent else c.textPrimary.copy(alpha = 0.06f))
                        .clickable { onSelect(n) }
                        .padding(vertical = ZenSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = n.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (on) c.gradient.first() else c.textPrimary
                    )
                }
            }
        }
    }
}

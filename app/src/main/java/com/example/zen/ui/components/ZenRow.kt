package com.example.zen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenSpacing

/**
 * The one row. Settings, onboarding, and home state all use it:
 * a title, an optional line, and a trailing control or mark.
 */
@Composable
fun ZenRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    highlighted: Boolean = false,
    muted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    asCard: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val click = if (onClick != null) {
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
    } else {
        Modifier
    }
    if (asCard) {
        GlassCard(
            modifier = modifier
                .fillMaxWidth()
                .then(click),
            pressed = onClick != null && pressed,
            highlighted = highlighted
        ) {
            ZenRowBody(title, description, muted, leading, trailing)
        }
    } else {
        Box(modifier = modifier.fillMaxWidth().then(click)) {
            ZenRowBody(title, description, muted, leading, trailing)
        }
    }
}

@Composable
private fun ZenRowBody(
    title: String,
    description: String?,
    muted: Boolean,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?
) {
    val c = LocalPersonaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(ZenSpacing.lg))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (muted) c.textSecondary else c.textPrimary
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(ZenSpacing.xs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
                    color = c.textSecondary
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(ZenSpacing.md))
            trailing()
        }
    }
}

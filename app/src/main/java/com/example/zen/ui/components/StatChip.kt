package com.example.zen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing

/**
 * A compact metric tile: big value (optionally prefixed by an accent icon) over a tracked caption.
 * Built on [GlassCard] so it shares the frosted surface + border of every other card.
 */
@Composable
fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val c = LocalPersonaColors.current
    GlassCard(modifier = modifier, shape = ZenRadius.chip, contentPadding = ZenSpacing.md) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(end = ZenSpacing.xs)
                    )
                }
                Text(value, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = ZenSpacing.xs)
            )
        }
    }
}

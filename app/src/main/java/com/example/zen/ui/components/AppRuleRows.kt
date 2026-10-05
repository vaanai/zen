package com.example.zen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.zen.data.KnownApps
import com.example.zen.data.RuleCopy
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenElevation
import com.example.zen.ui.design.ZenSpacing

/**
 * The four apps, in one row treatment.
 * [onToggle] null means state: a guarded app shows its limit, and this is not where it is edited.
 */
@Composable
fun AppRuleRows(
    isGuarded: (String) -> Boolean,
    friendPassEnabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: ((name: String, guarded: Boolean) -> Unit)? = null,
    grouped: Boolean = false
) {
    val c = LocalPersonaColors.current
    val rows = @Composable {
        KnownApps.apps.forEachIndexed { index, app ->
            if (grouped && index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ZenSpacing.md)
                        .height(ZenElevation.hairline)
                        .background(c.cardBorder)
                )
            }
            val on = isGuarded(app.name)
            val limit = if (on) RuleCopy.appLimit(app.name, friendPassEnabled) else null
            ZenRow(
                title = app.name,
                description = limit,
                muted = !on && onToggle == null,
                asCard = !grouped,
                onClick = onToggle?.let { toggle -> { toggle(app.name, !on) } },
                onClickLabel = if (onToggle != null) {
                    if (on) "Stop guarding ${app.name}" else "Guard ${app.name}"
                } else {
                    null
                },
                modifier = if (grouped) Modifier else Modifier.padding(bottom = ZenSpacing.md),
                trailing = {
                    if (onToggle != null) {
                        Switch(
                            checked = on,
                            onCheckedChange = { checked -> onToggle(app.name, checked) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent,
                                checkedThumbColor = c.onAccent,
                                uncheckedThumbColor = c.textPrimary,
                                uncheckedTrackColor = c.textPrimary.copy(alpha = 0.18f),
                                uncheckedBorderColor = c.textSecondary
                            )
                        )
                    } else if (on) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Guarded",
                            tint = c.safe
                        )
                    } else {
                        Text(
                            text = "Off",
                            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
                            color = c.textSecondary
                        )
                    }
                }
            )
        }
    }
    if (grouped) {
        GlassCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = ZenSpacing.xl)
        ) {
            rows()
        }
    } else {
        Column(modifier) { rows() }
    }
}

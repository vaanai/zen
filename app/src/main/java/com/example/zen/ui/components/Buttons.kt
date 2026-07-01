package com.example.zen.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenRadius

/** Filled primary action. Accent fill, contrast label, token radius + consistent height. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = LocalPersonaColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ZenRadius.pill,
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = c.gradient.first(),
            disabledContainerColor = c.accent.copy(alpha = 0.3f),
            disabledContentColor = c.gradient.first().copy(alpha = 0.5f)
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Outlined secondary action sharing the primary's shape/height. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = LocalPersonaColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ZenRadius.pill,
        modifier = modifier.height(52.dp),
        border = BorderStroke(1.dp, SolidColor(c.accent.copy(alpha = 0.6f))),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.accent)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

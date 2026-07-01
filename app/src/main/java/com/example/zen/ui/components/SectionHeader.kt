package com.example.zen.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenSpacing

/** All-caps, tracked section label — one consistent treatment for every list/section heading. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val c = LocalPersonaColors.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = c.textSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ZenSpacing.md)
    )
}

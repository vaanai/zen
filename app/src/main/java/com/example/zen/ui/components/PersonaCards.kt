package com.example.zen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.zen.persona.LineLibrary
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import com.example.zen.ui.design.ZenSpacing

/** The one voice picker. Onboarding and settings both use these cards. */
@Composable
fun PersonaCards(
    selected: Persona,
    onSelect: (Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalPersonaColors.current
    Column(modifier) {
        Persona.entries.forEach { persona ->
            val isSelected = persona == selected
            ZenRow(
                title = persona.displayName,
                description = persona.tagline,
                highlighted = isSelected,
                onClick = { onSelect(persona) },
                modifier = Modifier.padding(bottom = ZenSpacing.md),
                leading = {
                    Text(
                        text = persona.glyph,
                        style = TextStyle(fontFamily = FontFamily.Default, fontSize = 28.sp)
                    )
                },
                trailing = if (isSelected) {
                    @Composable {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = c.accent)
                    }
                } else {
                    null
                }
            )
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        Text(
            text = LineLibrary.welcome(selected),
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
            color = c.accent
        )
    }
}

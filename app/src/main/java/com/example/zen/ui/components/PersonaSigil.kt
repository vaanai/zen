package com.example.zen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.zen.persona.LocalPersona
import com.example.zen.persona.LocalPersonaColors

/**
 * Renders the active persona's glyph inside a consistent circular accent halo. This is the ONE
 * intentional place emoji glyphs appear — turning what was ad-hoc inline emoji into a deliberate
 * brand mark at a fixed size.
 */
@Composable
fun PersonaSigil(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    glyphSize: Dp = 22.dp
) {
    val c = LocalPersonaColors.current
    val persona = LocalPersona.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(c.accent.copy(alpha = 0.28f), c.accent.copy(alpha = 0.10f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Emoji glyphs must render with the system font, never the persona typeface.
        Text(
            text = persona.glyph,
            style = TextStyle(fontFamily = FontFamily.Default, fontSize = glyphSize.value.sp)
        )
    }
}

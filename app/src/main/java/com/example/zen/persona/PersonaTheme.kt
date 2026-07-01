package com.example.zen.persona

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.zen.theme.personaTypography

/** The active persona's raw colors, available anywhere in the UI tree. */
val LocalPersonaColors = staticCompositionLocalOf { PersonaPalette.of(Persona.DEFAULT) }

/** The active persona itself, available anywhere in the UI tree. */
val LocalPersona = staticCompositionLocalOf { Persona.DEFAULT }

/**
 * Applies a persona's full visual identity — Material color scheme, typography, and the
 * [LocalPersonaColors]/[LocalPersona] accents the custom UI reads. Switching persona reskins the
 * entire app.
 */
@Composable
fun PersonaTheme(persona: Persona, content: @Composable () -> Unit) {
    val colors = PersonaPalette.of(persona)

    val scheme = if (colors.isLight) {
        lightColorScheme(
            primary = colors.accent,
            secondary = colors.accentSecondary,
            tertiary = colors.danger,
            background = colors.gradient.first(),
            surface = colors.gradient.first(),
            onPrimary = colors.gradient.first(),
            onSecondary = colors.gradient.first(),
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        darkColorScheme(
            primary = colors.accent,
            secondary = colors.accentSecondary,
            tertiary = colors.danger,
            background = colors.gradient.first(),
            surface = colors.gradient[1],
            onPrimary = colors.textPrimary,
            onSecondary = colors.gradient.first(),
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(
        LocalPersonaColors provides colors,
        LocalPersona provides persona
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = personaTypography(colors.fontFamily),
            content = content
        )
    }
}

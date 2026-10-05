package com.example.zen.persona

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.zen.ui.design.FrauncesFamily
import com.example.zen.ui.design.InterFamily

/**
 * Single source of truth for a persona's visual identity. Used by both the Compose UI
 * (via PersonaTheme) and the Accessibility Service overlay (via [Color.toArgb]).
 */
data class PersonaColors(
    /** Background gradient, top → bottom (3 stops). */
    val gradient: List<Color>,
    val accent: Color,
    val accentSecondary: Color,
    val danger: Color,
    val safe: Color,
    val warn: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Label drawn on [accent]. Dark accents use a light ink; bright accents use a dark one. */
    val onAccent: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val fontFamily: FontFamily,
    /** True for light-background personas (Zen, Sage) — affects status-bar icon tint, etc. */
    val isLight: Boolean
)

object PersonaPalette {

    fun of(persona: Persona): PersonaColors = when (persona) {
        Persona.GOBLIN -> Goblin
        Persona.COACH -> Coach
        Persona.ZEN -> Zen
        Persona.SAGE -> Sage
    }

    // Goblin — the original "cosmic" dark/neon look.
    private val Goblin = PersonaColors(
        gradient = listOf(Color(0xFF07050E), Color(0xFF0D0B21), Color(0xFF1E113E)),
        accent = Color(0xFF8B5CF6),          // neon violet
        accentSecondary = Color(0xFF06B6D4), // cyber cyan
        danger = Color(0xFFEF4444),
        safe = Color(0xFF10B981),
        warn = Color(0xFFF59E0B),
        textPrimary = Color(0xFFF9FAFB),
        textSecondary = Color(0xFFC5CDD6),
        onAccent = Color(0xFF07050E),
        cardBackground = Color(0xF2161428),
        cardBorder = Color(0x33FFFFFF),
        fontFamily = InterFamily,
        isLight = false
    )

    // Coach — dark athletic base, electric lime + energetic orange.
    private val Coach = PersonaColors(
        gradient = listOf(Color(0xFF0A0F0A), Color(0xFF11210C), Color(0xFF1A2E10)),
        accent = Color(0xFFB6FF3C),          // electric lime
        accentSecondary = Color(0xFFFF6B35), // energetic orange
        danger = Color(0xFFFF4D4D),
        safe = Color(0xFFB6FF3C),
        warn = Color(0xFFFFC53D),
        textPrimary = Color(0xFFF7FFF0),
        textSecondary = Color(0xFFC5D6B8),
        onAccent = Color(0xFF0A0F0A),
        cardBackground = Color(0xF2142212),
        cardBorder = Color(0x33B6FF3C),
        fontFamily = InterFamily,
        isLight = false
    )

    // Zen — calm, rich, light beige.
    private val Zen = PersonaColors(
        gradient = listOf(Color(0xFFF4EEE2), Color(0xFFEBE1CF), Color(0xFFE1D5BE)),
        accent = Color(0xFF3E4A34),          // deep olive, readable on paper
        accentSecondary = Color(0xFF8C5E3C), // warm clay
        danger = Color(0xFF6E3A2C),
        safe = Color(0xFF3E4A34),
        warn = Color(0xFF6B4E16),
        textPrimary = Color(0xFF2C281F),
        textSecondary = Color(0xFF4A453C),
        onAccent = Color(0xFFF7F3EA),
        cardBackground = Color(0xF2FFF9F1),
        cardBorder = Color(0x263A352C),
        fontFamily = InterFamily,
        isLight = true
    )

    // Sage — parchment & ink, serif, old-world.
    private val Sage = PersonaColors(
        gradient = listOf(Color(0xFFEEE5D4), Color(0xFFE4D8C1), Color(0xFFD9CAAE)),
        accent = Color(0xFF6B4F3A),          // ink brown
        accentSecondary = Color(0xFF8C6A43), // aged bronze
        danger = Color(0xFF6E3A2C),
        safe = Color(0xFF3E4A32),
        warn = Color(0xFF6B4E16),
        textPrimary = Color(0xFF2E2519),
        textSecondary = Color(0xFF4A4032),
        onAccent = Color(0xFFF7F1E4),
        cardBackground = Color(0xF2FBF6EA),
        cardBorder = Color(0x2E2E2519),
        fontFamily = FrauncesFamily,
        isLight = true
    )
}

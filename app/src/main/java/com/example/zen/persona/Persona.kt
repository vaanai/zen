package com.example.zen.persona

/**
 * The four personalities. The selected persona reskins theme and typography.
 * A block keeps that color and type on one short note. The words do not change.
 * See [PersonaPalette] for visuals and [LineLibrary] for copy.
 */
enum class Persona(
    val id: String,
    val displayName: String,
    /** One-line pitch shown in the persona chooser. */
    val tagline: String,
    /** Short status badge shown on the dashboard, e.g. "GOBLIN MODE ACTIVE". */
    val statusBadge: String,
    /** Emoji used as the persona's avatar / shield glyph. */
    val glyph: String
) {
    GOBLIN(
        id = "GOBLIN",
        displayName = "The Goblin",
        tagline = "Dark color, plain type.",
        statusBadge = "GOBLIN MODE ACTIVE",
        glyph = "👺" // ogre
    ),
    COACH(
        id = "COACH",
        displayName = "The Coach",
        tagline = "Bright color, plain type.",
        statusBadge = "COACH MODE — TRAINING",
        glyph = "💪" // flexed biceps
    ),
    ZEN(
        id = "ZEN",
        displayName = "Zen",
        tagline = "Warm paper, quiet color.",
        statusBadge = "ZEN MODE",
        glyph = "🪷" // lotus
    ),
    SAGE(
        id = "SAGE",
        displayName = "The Sage",
        tagline = "Serif on parchment.",
        statusBadge = "THE SAGE IS WATCHING",
        glyph = "🏺" // amphora
    );

    companion object {
        val DEFAULT = GOBLIN

        fun fromId(id: String?): Persona =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.zen.ui.design

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.zen.R

/**
 * Bundled variable fonts. Two families cover all four personas — persona voice comes from the
 * type scale (weight / tracking / size), not from separate typefaces.
 *
 * These are variable fonts driven by [FontVariation]. On API < 26 the variation settings are
 * ignored and the font renders at its default instance (still legible) — an acceptable fallback
 * for the small tail of pre-Oreo devices (minSdk 24).
 */

private fun interFont(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private fun frauncesFont(weight: Int) = Font(
    resId = R.font.fraunces_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        // Optical size: nudge toward the high-contrast display cut for a refined serif voice.
        FontVariation.Setting("opsz", 32f)
    )
)

/** Workhorse UI/body typeface for every persona. */
val InterFamily = FontFamily(
    interFont(400),
    interFont(500),
    interFont(600),
    interFont(700),
    interFont(800)
)

/** Serif display/body voice — used only by the Sage persona. */
val FrauncesFamily = FontFamily(
    frauncesFont(400),
    frauncesFont(500),
    frauncesFont(600),
    frauncesFont(700)
)

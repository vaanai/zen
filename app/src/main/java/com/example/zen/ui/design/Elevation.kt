package com.example.zen.ui.design

import androidx.compose.ui.unit.dp

/**
 * Depth tokens. Light personas (Zen/Sage) read best with a soft ambient drop shadow; dark
 * personas (Goblin/Coach) read best with a faint accent glow. [com.example.zen.ui.components.GlassCard]
 * chooses between them based on the persona's `isLight` flag.
 */
object ZenElevation {
    /** Ambient drop-shadow radius for light-surface personas. */
    val ambient = 12.dp

    /** Accent-glow radius behind cards / the hero ring for dark-surface personas. */
    val glow = 24.dp

    /** Hairline border width used for the top-lit edge on glass surfaces. */
    val hairline = 1.dp
}

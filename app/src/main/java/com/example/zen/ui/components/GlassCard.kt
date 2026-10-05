package com.example.zen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenElevation
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * The [HazeState] that backing surfaces frost against. A screen sets this at its root (behind the
 * gradient); [GlassCard] reads it to blur the content beneath. Null → no blur source available, so
 * cards fall back to a plain translucent fill.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * The single card primitive for the app: a frosted-glass surface over the persona gradient with a
 * top-lit hairline border. Replaces the ad-hoc `Card + border` pattern. On devices without blur
 * support (API < 31) Haze draws the persona's translucent `cardBackground` scrim instead — matching
 * the app's previous look.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = ZenRadius.card,
    contentPadding: Dp = ZenSpacing.cardPadding,
    pressed: Boolean = false,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val c = LocalPersonaColors.current
    val haze = LocalHazeState.current
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "cardPress")

    val glass = Modifier
        .scale(scale)
        .shadow(
            elevation = ZenElevation.ambient,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (c.isLight) 0.12f else 0.36f),
            spotColor = Color.Black.copy(alpha = if (c.isLight) 0.08f else 0.28f)
        )
        .clip(shape)
        .then(
            if (haze != null) {
                Modifier.hazeEffect(
                    state = haze,
                    style = HazeStyle(
                        tints = listOf(HazeTint(c.cardBackground)),
                        blurRadius = 12.dp,
                        noiseFactor = 0f,
                        fallbackTint = HazeTint(c.cardBackground)
                    )
                )
            } else {
                Modifier.background(c.cardBackground)
            }
        )
        .border(
            width = if (highlighted) 2.dp else ZenElevation.hairline,
            brush = if (highlighted) SolidColor(c.accent) else topLitBorder(),
            shape = shape
        )

    Box(modifier = modifier.then(glass)) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}

/** A vertical gradient border — brighter at the top edge — that reads as a lit glass rim. */
@Composable
private fun topLitBorder(): Brush {
    val c = LocalPersonaColors.current
    return Brush.verticalGradient(
        listOf(
            c.textPrimary.copy(alpha = 0.22f),
            c.cardBorder
        )
    )
}

/** Remembers a pressed-state source that a caller can hand to [GlassCard] for scale feedback. */
@Composable
fun rememberPressedState(): Pair<MutableInteractionSource, Boolean> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return source to pressed
}

package com.example.zen.ui.design

import androidx.compose.ui.unit.dp

/**
 * The single spacing scale for the whole app — a 4dp base grid. Every padding / gap / spacer
 * should reference one of these instead of a raw `.dp` literal, so spacing stays consistent
 * across screens and personas.
 */
object ZenSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    // Semantic aliases built from the scale.
    val screenGutter = xl      // horizontal page margin
    val cardPadding = lg       // internal card padding
    val sectionGap = xl        // gap between major sections
    val itemGap = md           // gap between sibling cards/chips
}

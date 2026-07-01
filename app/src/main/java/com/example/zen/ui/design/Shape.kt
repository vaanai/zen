package com.example.zen.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale. Replaces the ad-hoc 4/12/16 values that were scattered inline.
 */
object ZenRadius {
    val chip = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val hero = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(50)
}

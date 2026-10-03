package com.example.zen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.zen.data.RuleCopy
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.ui.design.ZenSpacing

/**
 * The rule, then the limits the main sentence does not cover.
 * [emphasize] is the home hero: same words, set large enough to read while settings are locked.
 */
@Composable
fun RuleStatement(
    friendPassEnabled: Boolean,
    allowedScrolls: Int,
    tiktokGuarded: Boolean,
    youtubeGuarded: Boolean,
    includeLimits: Boolean,
    emphasize: Boolean,
    modifier: Modifier = Modifier
) {
    val c = LocalPersonaColors.current
    val sentenceStyle = if (emphasize) {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        )
    } else {
        MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.sp)
    }
    Column(modifier) {
        Text(
            text = RuleCopy.sentence(friendPassEnabled, allowedScrolls),
            style = sentenceStyle,
            color = c.textPrimary
        )
        if (includeLimits) {
            RuleCopy.limits(friendPassEnabled, tiktokGuarded, youtubeGuarded).forEach { line ->
                Spacer(Modifier.height(ZenSpacing.md))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
                    color = c.textSecondary
                )
            }
        }
    }
}

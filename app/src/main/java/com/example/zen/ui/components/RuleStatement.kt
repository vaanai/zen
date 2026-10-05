package com.example.zen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val sentence = RuleCopy.sentence(friendPassEnabled, allowedScrolls)
    val splitAt = if (emphasize) sentence.indexOf(". ") else -1
    val lead = if (splitAt > 0) sentence.substring(0, splitAt + 1) else sentence
    val rest = if (splitAt > 0) sentence.substring(splitAt + 2) else null
    Column(modifier) {
        Text(
            text = lead,
            style = if (emphasize) {
                MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = c.textPrimary
        )
        if (rest != null) {
            Spacer(Modifier.height(ZenSpacing.sm))
            Text(
                text = rest,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary
            )
        }
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

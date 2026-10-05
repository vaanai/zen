package com.example.zen.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.zen.data.GuardMode
import com.example.zen.data.KnownApps
import com.example.zen.data.RuleCopy
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import com.example.zen.ui.components.AppRuleRows
import com.example.zen.ui.components.LocalHazeState
import com.example.zen.ui.components.PersonaCards
import com.example.zen.ui.components.PrimaryButton
import com.example.zen.ui.components.RuleModes
import com.example.zen.ui.components.RuleStatement
import com.example.zen.ui.components.SecondaryButton
import com.example.zen.ui.components.SectionHeader
import com.example.zen.ui.components.ZenRow
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun OnboardingScreen(
    prefs: ZenPrefs,
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
    isAccessibilityEnabled: Boolean,
    isUsageEnabled: Boolean,
    onFinish: () -> Unit
) {
    val c = LocalPersonaColors.current
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(0) }
    val selectedApps = rememberSaveable(
        saver = listSaver(
            save = { apps: SnapshotStateList<String> -> apps.toList() },
            restore = { saved -> mutableStateListOf<String>().apply { addAll(saved) } }
        )
    ) {
        mutableStateListOf<String>().apply {
            addAll(
                KnownApps.apps
                    .filter { app -> app.packages.any { it in prefs.blockedPackages } }
                    .map { it.name }
            )
        }
    }
    var friendPass by rememberSaveable { mutableStateOf(true) }
    var allowedScrolls by rememberSaveable { mutableIntStateOf(0) }
    var password by rememberSaveable { mutableStateOf("") }

    val totalSteps = 4
    val hazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
            .hazeSource(hazeState)
    ) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ZenSpacing.screenGutter)
            ) {
                Text(
                    text = "Step ${step + 1} of $totalSteps".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(ZenSpacing.sm))
                LinearProgressIndicator(
                    progress = { (step + 1) / totalSteps.toFloat() },
                    color = c.accent,
                    trackColor = c.textPrimary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(ZenRadius.pill)
                )
                Spacer(Modifier.height(ZenSpacing.xl))

                Box(modifier = Modifier.weight(1f)) {
                    when (step) {
                        0 -> StepRule(
                            selectedApps = selectedApps,
                            friendPass = friendPass,
                            allowedScrolls = allowedScrolls,
                            onRuleChange = { pass, scrolls ->
                                friendPass = pass
                                allowedScrolls = scrolls
                            }
                        )
                        1 -> StepAccessibility(
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            isUsageEnabled = isUsageEnabled,
                            friendPass = friendPass,
                            allowedScrolls = allowedScrolls,
                            tiktokGuarded = "TikTok" in selectedApps,
                            youtubeGuarded = "YouTube" in selectedApps,
                            onOpenAccessibility = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onOpenUsage = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        )
                        2 -> StepVoice(selectedPersona, onPersonaSelected)
                        3 -> StepLock(password = password, onPasswordChange = { password = it })
                    }
                }

                Spacer(Modifier.height(ZenSpacing.lg))
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (step > 0) {
                        SecondaryButton(
                            text = "Back",
                            onClick = { step-- },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = ZenSpacing.md)
                        )
                    }
                    val onAccessibilityStep = step == 1
                    PrimaryButton(
                        text = if (step < totalSteps - 1) "Continue" else "Begin",
                        onClick = {
                            if (step < totalSteps - 1) {
                                step++
                            } else if (!onboardingMayFinish(isAccessibilityEnabled)) {
                                step = 1
                            } else {
                                commit(
                                    prefs = prefs,
                                    selectedAppNames = selectedApps,
                                    friendPassEnabled = friendPass,
                                    allowedScrolls = allowedScrolls,
                                    password = password,
                                    accessibilityEnabled = isAccessibilityEnabled
                                )
                                onFinish()
                            }
                        },
                        enabled = !onAccessibilityStep || isAccessibilityEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Onboarding finishes only while accessibility is on. A password is optional at any length.
 * A blank password keeps the cooldown as the only lock.
 */
internal fun onboardingMayFinish(accessibilityEnabled: Boolean): Boolean = accessibilityEnabled

private fun commit(
    prefs: ZenPrefs,
    selectedAppNames: List<String>,
    friendPassEnabled: Boolean,
    allowedScrolls: Int,
    password: String,
    accessibilityEnabled: Boolean
) {
    if (!onboardingMayFinish(accessibilityEnabled)) return
    val rule = GuardMode.kept(friendPassEnabled, allowedScrolls)
    prefs.blockedPackages = KnownApps.apps
        .filter { it.name in selectedAppNames }
        .flatMap { it.packages }
        .toSet()
    prefs.friendPassEnabled = rule.friendPassEnabled
    prefs.allowedScrolls = rule.allowedScrolls
    prefs.earnedScrollsEnabled = false
    prefs.lockPassword = password
    prefs.onboardingComplete = true
    prefs.lockNow()
}

@Composable
private fun StepTitle(title: String, subtitle: String? = null) {
    val c = LocalPersonaColors.current
    Text(title, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
    if (subtitle != null) {
        Spacer(Modifier.height(ZenSpacing.sm))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
    Spacer(Modifier.height(ZenSpacing.lg))
}

@Composable
private fun StepRule(
    selectedApps: SnapshotStateList<String>,
    friendPass: Boolean,
    allowedScrolls: Int,
    onRuleChange: (Boolean, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepTitle("The rule")
        RuleStatement(
            friendPassEnabled = friendPass,
            allowedScrolls = allowedScrolls,
            tiktokGuarded = "TikTok" in selectedApps,
            youtubeGuarded = "YouTube" in selectedApps,
            includeLimits = false,
            emphasize = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(ZenSpacing.lg))
        RuleModes(
            friendPassEnabled = friendPass,
            allowedScrolls = allowedScrolls,
            onChange = onRuleChange
        )
        Spacer(Modifier.height(ZenSpacing.sm))
        SectionHeader("Apps")
        AppRuleRows(
            isGuarded = { it in selectedApps },
            friendPassEnabled = friendPass,
            onToggle = { name, checked ->
                if (checked) {
                    if (name !in selectedApps) selectedApps.add(name)
                } else {
                    selectedApps.removeAll { it == name }
                }
            }
        )
    }
}

@Composable
private fun StepAccessibility(
    isAccessibilityEnabled: Boolean,
    isUsageEnabled: Boolean,
    friendPass: Boolean,
    allowedScrolls: Int,
    tiktokGuarded: Boolean,
    youtubeGuarded: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit
) {
    val c = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepTitle(
            title = "Accessibility",
            subtitle = "Required. Zen can't keep this rule while this is off."
        )
        ZenRow(
            title = "Accessibility",
            description = RuleCopy.stated(
                friendPassEnabled = friendPass,
                allowedScrolls = allowedScrolls,
                tiktokGuarded = tiktokGuarded,
                youtubeGuarded = youtubeGuarded
            ),
            highlighted = isAccessibilityEnabled,
            onClick = onOpenAccessibility,
            onClickLabel = "Grant accessibility",
            trailing = {
                if (isAccessibilityEnabled) {
                    Text("On", style = MaterialTheme.typography.titleMedium, color = c.safe)
                } else {
                    Text("Grant", style = MaterialTheme.typography.labelLarge, color = c.accent)
                }
            }
        )
        TextButton(onClick = onOpenUsage, modifier = Modifier.padding(top = ZenSpacing.sm)) {
            Text(
                text = if (isUsageEnabled) "Usage access is on" else "Usage access is optional",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary
            )
        }
        if (!isAccessibilityEnabled) {
            Spacer(Modifier.height(ZenSpacing.lg))
            Text(
                text = "Accessibility is off. Zen can't keep this rule until it's on.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.warn
            )
        }
    }
}

@Composable
private fun StepVoice(selected: Persona, onSelect: (Persona) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepTitle(
            title = "Voice",
            subtitle = "Color and type only. A block still says one line, and the rule stays the same."
        )
        PersonaCards(selected = selected, onSelect = onSelect)
    }
}

@Composable
private fun StepLock(password: String, onPasswordChange: (String) -> Unit) {
    val c = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepTitle(
            title = "Lock the rule",
            subtitle = "A password is optional. Leave it blank and changing the rule takes a 2-minute wait."
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.textPrimary,
                unfocusedTextColor = c.textPrimary,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.textSecondary,
                focusedLabelColor = c.accent,
                unfocusedLabelColor = c.textSecondary,
                cursorColor = c.accent,
                focusedContainerColor = c.cardBackground,
                unfocusedContainerColor = c.cardBackground
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

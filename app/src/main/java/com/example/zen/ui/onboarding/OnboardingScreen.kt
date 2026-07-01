package com.example.zen.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zen.data.KnownApps
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LineLibrary
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import com.example.zen.ui.components.GlassCard
import com.example.zen.ui.components.LocalHazeState
import com.example.zen.ui.components.PrimaryButton
import com.example.zen.ui.components.SecondaryButton
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

    val selectedApps = remember {
        mutableStateListOf<String>().apply {
            addAll(KnownApps.apps.filter { app -> app.packages.any { it in prefs.blockedPackages } }.map { it.name })
        }
    }
    var friendPass by rememberSaveable { mutableStateOf(prefs.friendPassEnabled) }
    var dailyCap by rememberSaveable { mutableStateOf(prefs.dailyCapMinutes.toFloat()) }
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
                        0 -> StepPersona(selectedPersona, onPersonaSelected)
                        1 -> StepPermissions(
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            isUsageEnabled = isUsageEnabled,
                            onOpenAccessibility = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            onOpenUsage = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        )
                        2 -> StepConfig(
                            selectedApps = selectedApps,
                            friendPass = friendPass,
                            onFriendPassChange = { friendPass = it },
                            dailyCap = dailyCap,
                            onDailyCapChange = { dailyCap = it }
                        )
                        3 -> StepLock(password = password, onPasswordChange = { password = it })
                    }
                }

                Spacer(Modifier.height(ZenSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ZenSpacing.md)
                ) {
                    if (step > 0) {
                        SecondaryButton("Back", onClick = { step-- }, modifier = Modifier.weight(1f))
                    }
                    PrimaryButton(
                        text = if (step < totalSteps - 1) "Continue" else "Begin",
                        onClick = {
                            if (step < totalSteps - 1) {
                                step++
                            } else {
                                commit(prefs, selectedApps, friendPass, dailyCap.toInt(), password)
                                onFinish()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun commit(
    prefs: ZenPrefs,
    selectedAppNames: List<String>,
    friendPass: Boolean,
    dailyCap: Int,
    password: String
) {
    val packages = KnownApps.apps
        .filter { it.name in selectedAppNames }
        .flatMap { it.packages }
        .toSet()
    prefs.blockedPackages = packages
    prefs.friendPassEnabled = friendPass
    prefs.dailyCapMinutes = dailyCap
    if (password.length == ZenPrefs.PASSWORD_LENGTH) {
        prefs.lockPassword = password
    }
    prefs.onboardingComplete = true
    prefs.lockNow()
}

@Composable
private fun StepTitle(title: String, subtitle: String? = null) {
    val c = LocalPersonaColors.current
    Text(title, style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.sp), color = c.textPrimary)
    if (subtitle != null) {
        Spacer(Modifier.height(ZenSpacing.sm))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
    Spacer(Modifier.height(ZenSpacing.lg))
}

@Composable
private fun StepPersona(selected: Persona, onSelect: (Persona) -> Unit) {
    val c = LocalPersonaColors.current
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(
            "Choose your guardian",
            "This sets the whole vibe — colors, tone, and how it talks you off the feed. Change it anytime."
        )
        Persona.entries.forEach { p ->
            val isSel = p == selected
            val source = remember { MutableInteractionSource() }
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = ZenSpacing.md)
                    .clip(ZenRadius.card)
                    .then(
                        if (isSel) Modifier.border(2.dp, c.accent, ZenRadius.card) else Modifier
                    )
                    .clickable(interactionSource = source, indication = null) { onSelect(p) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.glyph, style = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Default, fontSize = 28.sp))
                    Spacer(Modifier.width(ZenSpacing.lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.displayName, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                        Text(p.tagline, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                    if (isSel) Icon(Icons.Default.CheckCircle, "Selected", tint = c.accent)
                }
            }
        }
        Spacer(Modifier.height(ZenSpacing.sm))
        Text(
            LineLibrary.welcome(selected),
            style = MaterialTheme.typography.bodyMedium,
            color = c.accent
        )
    }
}

@Composable
private fun StepPermissions(
    isAccessibilityEnabled: Boolean,
    isUsageEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(
            "Grant access",
            "Zen needs two permissions to work. Everything stays on your device — nothing is uploaded."
        )
        PermissionRow(
            title = "Accessibility Service",
            desc = "Lets Zen see when you're scrolling a short-form feed so it can step in. Zen does not read your messages or collect content.",
            granted = isAccessibilityEnabled,
            onClick = onOpenAccessibility
        )
        Spacer(Modifier.height(ZenSpacing.md))
        PermissionRow(
            title = "Usage Access",
            desc = "Lets Zen show your screen-time stats. Optional, but the dashboard is nicer with it.",
            granted = isUsageEnabled,
            onClick = onOpenUsage
        )
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, granted: Boolean, onClick: () -> Unit) {
    val c = LocalPersonaColors.current
    val source = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ZenRadius.card)
            .then(if (granted) Modifier.border(1.dp, c.safe, ZenRadius.card) else Modifier)
            .clickable(interactionSource = source, indication = null) { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Spacer(Modifier.height(ZenSpacing.xs))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
            Spacer(Modifier.width(ZenSpacing.md))
            if (granted) {
                Icon(Icons.Default.CheckCircle, "Granted", tint = c.safe)
            } else {
                Text("Grant", style = MaterialTheme.typography.labelLarge, color = c.accent)
            }
        }
    }
}

@Composable
private fun StepConfig(
    selectedApps: MutableList<String>,
    friendPass: Boolean,
    onFriendPassChange: (Boolean) -> Unit,
    dailyCap: Float,
    onDailyCapChange: (Float) -> Unit
) {
    val c = LocalPersonaColors.current
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle("What should I guard?")
        KnownApps.apps.forEach { app ->
            val checked = app.name in selectedApps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ZenSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.name, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = checked,
                    onCheckedChange = { on -> if (on) selectedApps.add(app.name) else selectedApps.remove(app.name) },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent)
                )
            }
        }
        Spacer(Modifier.height(ZenSpacing.lg))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Friend Pass", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    "Allow the one video a friend sent you in DMs — block the moment you scroll past it.",
                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary
                )
            }
            Switch(
                checked = friendPass,
                onCheckedChange = onFriendPassChange,
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent)
            )
        }
        Spacer(Modifier.height(ZenSpacing.xl))
        Text("Daily screen-time goal: ${dailyCap.toInt()} min", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Slider(
            value = dailyCap,
            onValueChange = onDailyCapChange,
            valueRange = 15f..240f,
            steps = 14,
            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent)
        )
    }
}

@Composable
private fun StepLock(password: String, onPasswordChange: (String) -> Unit) {
    val c = LocalPersonaColors.current
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(
            "Lock in your commitment",
            "Set a ${ZenPrefs.PASSWORD_LENGTH}-character password to protect your settings from your future weak-willed self. " +
                "Forget it? You can still change settings — but only after a 2-minute cooldown. Skipping the password keeps the cooldown as your lock."
        )
        OutlinedTextField(
            value = password,
            onValueChange = { if (it.length <= ZenPrefs.PASSWORD_LENGTH) onPasswordChange(it) },
            label = { Text("Password (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            supportingText = { Text("${password.length} / ${ZenPrefs.PASSWORD_LENGTH}") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                focusedLabelColor = c.accent,
                cursorColor = c.accent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (password.isNotEmpty() && password.length != ZenPrefs.PASSWORD_LENGTH) {
            Text(
                "Use exactly ${ZenPrefs.PASSWORD_LENGTH} characters, or leave blank to skip.",
                style = MaterialTheme.typography.bodySmall, color = c.warn
            )
        }
    }
}

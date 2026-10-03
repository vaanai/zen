package com.example.zen.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.zen.data.KnownApps
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import com.example.zen.ui.components.AppRuleRows
import com.example.zen.ui.components.GlassCard
import com.example.zen.ui.components.LocalHazeState
import com.example.zen.ui.components.PersonaCards
import com.example.zen.ui.components.PrimaryButton
import com.example.zen.ui.components.RuleModes
import com.example.zen.ui.components.RuleStatement
import com.example.zen.ui.components.SecondaryButton
import com.example.zen.ui.components.SectionHeader
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    prefs: ZenPrefs,
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalPersonaColors.current

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            prefs.completeCooldownIfReady()
            tick++
            delay(1000)
        }
    }
    val unlocked = remember(tick) { prefs.isUnlocked() }
    val hazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
            .hazeSource(hazeState)
    ) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ZenSpacing.md, vertical = ZenSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ZenSpacing.screenGutter)
                ) {
                    if (!unlocked) {
                        LockGate(prefs, tick)
                    } else {
                        UnlockedSettings(prefs, selectedPersona, onPersonaSelected)
                    }
                    Spacer(Modifier.height(ZenSpacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun LockGate(prefs: ZenPrefs, tick: Int) {
    val c = LocalPersonaColors.current
    var attempt by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val cooldownPending = remember(tick) { prefs.isCooldownPending() }
    val remaining = remember(tick) { prefs.cooldownRemainingMs() }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ZenSpacing.xl),
        shape = ZenRadius.hero,
        contentPadding = ZenSpacing.xl
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RuleStatement(
                friendPassEnabled = prefs.friendPassEnabled,
                allowedScrolls = prefs.allowedScrolls,
                tiktokGuarded = prefs.tiktokGuarded,
                youtubeGuarded = prefs.youtubeGuarded,
                includeLimits = true,
                emphasize = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(ZenSpacing.xl))
            Icon(Icons.Default.Lock, contentDescription = null, tint = c.accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(ZenSpacing.md))
            Text("This rule is locked", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Spacer(Modifier.height(ZenSpacing.sm))
            Text(
                text = "You committed to this on purpose. Changing it should take a moment of intention.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(ZenSpacing.xl))

            if (cooldownPending) {
                Text(
                    text = "Unlocking in ${formatMs(remaining)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.accent
                )
                Spacer(Modifier.height(ZenSpacing.xs))
                Text(
                    text = "The wait keeps going if you leave. It unlocks when you come back to this screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(ZenSpacing.md))
                TextButton(onClick = { prefs.cancelCooldown() }) {
                    Text("Cancel", color = c.textSecondary)
                }
            } else if (prefs.hasPassword()) {
                OutlinedTextField(
                    value = attempt,
                    onValueChange = {
                        attempt = it
                        error = false
                    },
                    label = { Text("Enter password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text("Wrong password.", style = MaterialTheme.typography.bodySmall, color = c.danger)
                }
                Spacer(Modifier.height(ZenSpacing.md))
                PrimaryButton(
                    text = "Unlock",
                    onClick = { if (!prefs.tryPassword(attempt)) error = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(ZenSpacing.sm))
                TextButton(onClick = { prefs.beginCooldown() }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Forgot password? Unlock after 2 minutes",
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "No password set — unlocking just takes a 2-minute wait.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(ZenSpacing.md))
                PrimaryButton(
                    text = "Start 2-minute unlock",
                    onClick = { prefs.beginCooldown() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun UnlockedSettings(
    prefs: ZenPrefs,
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit
) {
    val c = LocalPersonaColors.current
    val selectedApps = remember {
        mutableStateListOf<String>().apply {
            addAll(
                KnownApps.apps
                    .filter { app -> app.packages.any { it in prefs.blockedPackages } }
                    .map { it.name }
            )
        }
    }
    var friendPass by remember { mutableStateOf(prefs.friendPassEnabled) }
    var allowedScrolls by remember { mutableIntStateOf(prefs.allowedScrolls) }
    var newPassword by remember { mutableStateOf(prefs.lockPassword) }
    var showPassword by remember { mutableStateOf(false) }

    fun writeApps() {
        prefs.blockedPackages = KnownApps.apps
            .filter { it.name in selectedApps }
            .flatMap { it.packages }
            .toSet()
    }

    fun writeRule(pass: Boolean, scrolls: Int) {
        friendPass = pass
        allowedScrolls = scrolls
        prefs.friendPassEnabled = pass
        prefs.allowedScrolls = scrolls
        prefs.earnedScrollsEnabled = false
    }

    Spacer(Modifier.height(ZenSpacing.sm))
    SectionHeader("The rule")
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
        onChange = ::writeRule
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
            writeApps()
        }
    )

    Spacer(Modifier.height(ZenSpacing.sm))
    SectionHeader("Voice")
    PersonaCards(
        selected = selectedPersona,
        onSelect = { persona ->
            prefs.persona = persona
            onPersonaSelected(persona)
        }
    )

    Spacer(Modifier.height(ZenSpacing.xl))
    SectionHeader("Lock")
    Text(
        text = "A password is optional. Leave it blank and the 2-minute wait is the lock.",
        style = MaterialTheme.typography.bodyMedium,
        color = c.textSecondary
    )
    Spacer(Modifier.height(ZenSpacing.md))
    OutlinedTextField(
        value = newPassword,
        onValueChange = { newPassword = it },
        label = { Text("Password (optional)") },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        trailingIcon = {
            TextButton(onClick = { showPassword = !showPassword }) {
                Text(if (showPassword) "Hide" else "Show", color = c.accent)
            }
        },
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    PrimaryButton(
        text = "Save password",
        onClick = { prefs.lockPassword = newPassword },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ZenSpacing.sm)
    )

    Spacer(Modifier.height(ZenSpacing.xl))
    SecondaryButton(
        text = "Lock settings now",
        onClick = { prefs.lockNow() },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun fieldColors(): androidx.compose.material3.TextFieldColors {
    val c = LocalPersonaColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.accent,
        focusedLabelColor = c.accent,
        cursorColor = c.accent
    )
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(java.util.Locale.US, "%d:%02d", m, s)
}

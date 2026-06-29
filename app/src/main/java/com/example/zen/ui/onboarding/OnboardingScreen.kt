package com.example.zen.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zen.data.KnownApps
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LineLibrary
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona

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

    // Quick-config state, seeded from current prefs.
    val selectedApps = remember {
        mutableStateListOf<String>().apply {
            addAll(KnownApps.apps.filter { app -> app.packages.any { it in prefs.blockedPackages } }.map { it.name })
        }
    }
    var friendPass by rememberSaveable { mutableStateOf(prefs.friendPassEnabled) }
    var dailyCap by rememberSaveable { mutableStateOf(prefs.dailyCapMinutes.toFloat()) }
    var password by rememberSaveable { mutableStateOf("") }

    val totalSteps = 4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Progress
            Text(
                text = "STEP ${step + 1} OF $totalSteps",
                color = c.textSecondary,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                color = c.accent,
                trackColor = c.textPrimary.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Spacer(Modifier.height(20.dp))

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

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                }
                Button(
                    onClick = {
                        if (step < totalSteps - 1) {
                            step++
                        } else {
                            commit(prefs, selectedApps, friendPass, dailyCap.toInt(), password)
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                    modifier = Modifier.weight(if (step > 0) 1f else 1f)
                ) {
                    Text(
                        text = if (step < totalSteps - 1) "Continue" else "Begin",
                        color = c.gradient.first(),
                        fontWeight = FontWeight.Bold
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
private fun StepPersona(selected: Persona, onSelect: (Persona) -> Unit) {
    val c = LocalPersonaColors.current
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Choose your guardian", color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "This sets the whole vibe — colors, tone, and how it talks you off the feed. Change it anytime.",
            color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        Persona.entries.forEach { p ->
            val isSel = p == selected
            Card(
                colors = CardDefaults.cardColors(containerColor = c.cardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) c.accent else c.cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(p) }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.glyph, fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.displayName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(p.tagline, color = c.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    if (isSel) {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = c.accent)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            LineLibrary.welcome(selected),
            color = c.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
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
    val c = LocalPersonaColors.current
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Grant access", color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Zen needs two permissions to work. Everything stays on your device — nothing is uploaded.",
            color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow(
            title = "Accessibility Service",
            desc = "Lets Zen see when you're scrolling a short-form feed so it can step in. Zen does not read your messages or collect content.",
            granted = isAccessibilityEnabled,
            onClick = onOpenAccessibility
        )
        Spacer(Modifier.height(12.dp))
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
    Card(
        colors = CardDefaults.cardColors(containerColor = c.cardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (granted) c.safe else c.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(desc, color = c.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Icon(Icons.Default.CheckCircle, "Granted", tint = c.safe)
            } else {
                Text("Grant", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
        Text("What should I guard?", color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        KnownApps.apps.forEach { app ->
            val checked = app.name in selectedApps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.name, color = c.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = checked,
                    onCheckedChange = { on ->
                        if (on) selectedApps.add(app.name) else selectedApps.remove(app.name)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Friend Pass", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Allow the one video a friend sent you in DMs — block the moment you scroll past it.",
                    color = c.textSecondary, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
            Switch(
                checked = friendPass,
                onCheckedChange = onFriendPassChange,
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Daily screen-time goal: ${dailyCap.toInt()} min", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        Text("Lock in your commitment", color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Set a ${ZenPrefs.PASSWORD_LENGTH}-character password to protect your settings from your future weak-willed self. " +
                "Forget it? You can still change settings — but only after a 2-minute cooldown. Skipping the password keeps the cooldown as your lock.",
            color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
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
                color = c.warn, fontSize = 12.sp
            )
        }
    }
}

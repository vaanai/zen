package com.example.zen.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zen.data.KnownApps
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    prefs: ZenPrefs,
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalPersonaColors.current

    // Tick every second so the lock countdown updates and a finished cooldown auto-unlocks.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            prefs.completeCooldownIfReady()
            tick++
            delay(1000)
        }
    }
    val unlocked = remember(tick) { prefs.isUnlocked() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.textPrimary)
                }
                Text("Settings", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                if (!unlocked) {
                    LockGate(prefs, tick)
                } else {
                    UnlockedSettings(prefs, selectedPersona, onPersonaSelected)
                }
                Spacer(Modifier.height(32.dp))
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

    Card(
        colors = CardDefaults.cardColors(containerColor = c.cardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .border(1.dp, c.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null, tint = c.accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Settings are locked", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "You committed to this on purpose. Changing it should take a moment of intention.",
                color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            if (cooldownPending) {
                Text("Unlocking in ${formatMs(remaining)}", color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Stay on this screen — it'll open automatically.", color = c.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { prefs.cancelCooldown() }) { Text("Cancel", color = c.textSecondary) }
            } else {
                if (prefs.hasPassword()) {
                    OutlinedTextField(
                        value = attempt,
                        onValueChange = { attempt = it; error = false },
                        label = { Text("Enter password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = error,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent, focusedLabelColor = c.accent, cursorColor = c.accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) Text("Wrong password.", color = c.danger, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { if (!prefs.tryPassword(attempt)) error = true },
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Unlock", color = c.gradient.first(), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { prefs.beginCooldown() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Forgot password? Unlock after 2 minutes", color = c.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    Text(
                        "No password set — unlocking just takes a 2-minute wait.",
                        color = c.textSecondary, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { prefs.beginCooldown() },
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start 2-minute unlock", color = c.gradient.first(), fontWeight = FontWeight.Bold) }
                }
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

    // Local mirrors of prefs so the UI reacts immediately; each change writes through.
    val selectedApps = remember {
        mutableStateListOf<String>().apply {
            addAll(KnownApps.apps.filter { app -> app.packages.any { it in prefs.blockedPackages } }.map { it.name })
        }
    }
    var friendPass by remember { mutableStateOf(prefs.friendPassEnabled) }
    var allowedScrolls by remember { mutableStateOf(prefs.allowedScrolls.toFloat()) }
    var dailyCap by remember { mutableStateOf(prefs.dailyCapMinutes.toFloat()) }
    var earned by remember { mutableStateOf(prefs.earnedScrollsEnabled) }
    var newPassword by remember { mutableStateOf(prefs.lockPassword) }
    var showPassword by remember { mutableStateOf(false) }

    fun writeApps() {
        prefs.blockedPackages = KnownApps.apps.filter { it.name in selectedApps }.flatMap { it.packages }.toSet()
    }

    Spacer(Modifier.height(8.dp))
    Section("PERSONA")
    Persona.entries.forEach { p ->
        val isSel = p == selectedPersona
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { prefs.persona = p; onPersonaSelected(p) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(p.glyph, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(p.displayName, color = c.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (isSel) Icon(Icons.Default.CheckCircle, "Selected", tint = c.accent)
        }
    }

    Spacer(Modifier.height(20.dp))
    Section("GUARDED APPS")
    KnownApps.apps.forEach { app ->
        ToggleRow(app.name, null, app.name in selectedApps) { on ->
            if (on) selectedApps.add(app.name) else selectedApps.remove(app.name)
            writeApps()
        }
    }

    Spacer(Modifier.height(20.dp))
    Section("BLOCKING")
    ToggleRow(
        "Friend Pass",
        "Allow the one video a friend DM'd you; block the next scroll.",
        friendPass
    ) { friendPass = it; prefs.friendPassEnabled = it }
    Spacer(Modifier.height(12.dp))
    Text(
        if (allowedScrolls.toInt() == 0) "Strictness: block the moment you open a feed"
        else "Strictness: allow ${allowedScrolls.toInt()} scroll(s) before blocking",
        color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
    )
    Slider(
        value = allowedScrolls,
        onValueChange = { allowedScrolls = it; prefs.allowedScrolls = it.toInt() },
        valueRange = 0f..5f,
        steps = 4,
        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent)
    )

    Spacer(Modifier.height(8.dp))
    Section("GOALS")
    Text("Daily screen-time goal: ${dailyCap.toInt()} min", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Slider(
        value = dailyCap,
        onValueChange = { dailyCap = it; prefs.dailyCapMinutes = it.toInt() },
        valueRange = 15f..240f,
        steps = 14,
        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent)
    )
    ToggleRow(
        "Lenient mode",
        "Give yourself a little extra grace before a block kicks in, instead of an outright wall. Off = strict.",
        earned
    ) { earned = it; prefs.earnedScrollsEnabled = it }

    Spacer(Modifier.height(20.dp))
    Section("COMMITMENT LOCK")
    OutlinedTextField(
        value = newPassword,
        onValueChange = { if (it.length <= ZenPrefs.PASSWORD_LENGTH) newPassword = it },
        label = { Text("Password (${ZenPrefs.PASSWORD_LENGTH} chars, blank = none)") },
        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        supportingText = { Text("${newPassword.length} / ${ZenPrefs.PASSWORD_LENGTH}") },
        trailingIcon = {
            TextButton(onClick = { showPassword = !showPassword }) {
                Text(if (showPassword) "Hide" else "Show", color = c.accent, fontSize = 12.sp)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent, focusedLabelColor = c.accent, cursorColor = c.accent
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = {
            if (newPassword.isEmpty() || newPassword.length == ZenPrefs.PASSWORD_LENGTH) {
                prefs.lockPassword = newPassword
            }
        },
        enabled = newPassword.isEmpty() || newPassword.length == ZenPrefs.PASSWORD_LENGTH,
        colors = ButtonDefaults.buttonColors(containerColor = c.accent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) { Text("Save password", color = c.gradient.first(), fontWeight = FontWeight.Bold) }

    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = { prefs.lockNow() }, modifier = Modifier.fillMaxWidth()) {
        Text("Lock settings now", color = c.textPrimary)
    }
}

@Composable
private fun Section(text: String) {
    val c = LocalPersonaColors.current
    Text(
        text, color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalPersonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (desc != null) Text(desc, color = c.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent)
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(java.util.Locale.US, "%d:%02d", m, s)
}

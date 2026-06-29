package com.example.zen.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zen.persona.LineLibrary
import com.example.zen.persona.LocalPersona
import com.example.zen.persona.LocalPersonaColors

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LocalPersonaColors.current
    val persona = LocalPersona.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
        ) {
            // Header row: persona name + settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = persona.displayName.uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = c.accent.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = persona.statusBadge,
                                color = c.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = c.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hero ring: saves today, ring fills toward the daily screen-time cap.
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(220.dp)
                        .padding(16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = c.textPrimary.copy(alpha = 0.06f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        val limit = uiState.dailyCapMinutes.toFloat().coerceAtLeast(1f)
                        val spent = uiState.totalTimeSpentMinutes.toFloat()
                        val sweep = ((spent / limit) * 360f).coerceIn(0f, 360f)
                        val overCap = spent >= limit
                        drawArc(
                            brush = Brush.sweepGradient(
                                if (overCap) listOf(c.danger, c.warn, c.danger)
                                else listOf(c.accentSecondary, c.accent, c.accentSecondary)
                            ),
                            startAngle = -90f,
                            sweepAngle = if (sweep <= 0f) 2f else sweep,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.savesToday}",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                            lineHeight = 52.sp
                        )
                        Text(
                            text = LineLibrary.savesLabel(persona),
                            fontSize = 10.sp,
                            color = c.textSecondary,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Stat chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip("🔥 ${uiState.streakDays}", "DAY STREAK", Modifier.weight(1f))
                    StatChip("${uiState.totalTimeSpentMinutes}m", "TODAY / ${uiState.dailyCapMinutes}m", Modifier.weight(1f))
                    StatChip("${uiState.savesTotal}", "ALL-TIME", Modifier.weight(1f))
                }
            }

            // Shield description (persona-voiced)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = c.cardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, c.cardBorder, RoundedCornerShape(16.dp))
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = c.accent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(persona.glyph, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = LineLibrary.shieldTitle(persona),
                                color = c.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = LineLibrary.shieldDescription(persona),
                                color = c.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Permissions
            item {
                SectionLabel("SYSTEM SETTINGS")
            }
            item {
                PermissionCard(
                    title = "Accessibility Blocker Service",
                    description = "Required to detect and block Reels / Shorts.",
                    isActive = uiState.isAccessibilityEnabled,
                    pulseAlpha = pulseAlpha,
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                PermissionCard(
                    title = "Screen Time Usage Access",
                    description = "Required for screen-time stats on this dashboard.",
                    isActive = uiState.isUsageAccessEnabled,
                    pulseAlpha = pulseAlpha,
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Per-app screen time
            if (uiState.isUsageAccessEnabled && uiState.appStatsList.isNotEmpty()) {
                item { SectionLabel("DETAILED SCREEN TIME") }
                items(uiState.appStatsList) { app ->
                    AppStatsCard(app = app)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = LocalPersonaColors.current
    Text(
        text = text,
        fontSize = 12.sp,
        color = c.textSecondary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    val c = LocalPersonaColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = c.cardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, c.cardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                color = c.textSecondary,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isActive: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val c = LocalPersonaColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = c.cardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, color = c.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (isActive) {
                Icon(Icons.Default.CheckCircle, "Active", tint = c.safe, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    Icons.Default.Warning,
                    "Pending",
                    tint = c.warn.copy(alpha = pulseAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AppStatsCard(app: com.example.zen.data.AppUsageItem) {
    val c = LocalPersonaColors.current
    val appColor = remember(app.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(app.colorHex))
        } catch (e: Exception) {
            c.accent
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = c.cardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.cardBorder.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(appColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(app.appName, color = c.textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                Text("${app.timeSpentMinutes} mins", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = remember(app.timeSpentMinutes) {
                (app.timeSpentMinutes / 60f).coerceIn(0.02f, 1f)
            }
            LinearProgressIndicator(
                progress = { progress },
                color = appColor,
                trackColor = c.textPrimary.copy(alpha = 0.05f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

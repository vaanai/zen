package com.example.zen.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zen.data.AppUsageItem
import com.example.zen.data.ZenStatsSource
import com.example.zen.data.ZenStatusProvider
import com.example.zen.persona.Persona
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isUsageAccessEnabled: Boolean = false,
    val totalTimeSpentMinutes: Long = 0L,
    val appStatsList: List<AppUsageItem> = emptyList(),
    val persona: Persona = Persona.DEFAULT,
    val streakDays: Int = 0,
    val savesToday: Int = 0,
    val savesTotal: Int = 0,
    val dailyCapMinutes: Int = 60,
    val earnedScrollsEnabled: Boolean = false,
    val earnedBalanceSeconds: Int = 0
)

class MainScreenViewModel(
    private val statusProvider: ZenStatusProvider,
    private val stats: ZenStatsSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        stats.touchActiveDay()
        startTracking()
    }

    fun refreshState() {
        val hasUsage = statusProvider.isUsageAccessEnabled()
        val hasAccessibility = statusProvider.isAccessibilityEnabled()
        val usageStats = if (hasUsage) statusProvider.getDetailedUsageStats() else emptyList()
        val totalMins = usageStats.sumOf { it.timeSpentMinutes }

        _uiState.update {
            it.copy(
                isAccessibilityEnabled = hasAccessibility,
                isUsageAccessEnabled = hasUsage,
                totalTimeSpentMinutes = totalMins,
                appStatsList = usageStats,
                persona = stats.persona,
                streakDays = stats.streakDays,
                savesToday = stats.savesToday(),
                savesTotal = stats.savesTotal,
                dailyCapMinutes = stats.dailyCapMinutes,
                earnedScrollsEnabled = stats.earnedScrollsEnabled,
                earnedBalanceSeconds = stats.earnedBalanceSeconds
            )
        }
    }

    private fun startTracking() {
        viewModelScope.launch {
            while (true) {
                refreshState()
                delay(3000) // refresh periodically for live permission / stat updates
            }
        }
    }
}

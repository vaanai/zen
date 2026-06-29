package com.example.zen.ui.main

import com.example.zen.data.AppUsageItem
import com.example.zen.data.ZenStatsSource
import com.example.zen.data.ZenStatusProvider
import com.example.zen.persona.Persona
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoadsCorrectState() = runTest {
    val fakeProvider = FakeZenStatusProvider(
        accessibilityEnabled = true,
        usageAccessEnabled = true,
        stats = listOf(AppUsageItem("com.instagram.android", "Instagram", 15L, "#E1306C"))
    )
    val fakeStats = FakeStatsSource(persona = Persona.SAGE, streakDays = 4, savesTotal = 12, savesToday = 3)
    val viewModel = MainScreenViewModel(fakeProvider, fakeStats)
    viewModel.refreshState()
    val state = viewModel.uiState.value
    assertEquals(true, state.isAccessibilityEnabled)
    assertEquals(true, state.isUsageAccessEnabled)
    assertEquals(15L, state.totalTimeSpentMinutes)
    assertEquals(1, state.appStatsList.size)
    assertEquals("Instagram", state.appStatsList[0].appName)
    assertEquals(Persona.SAGE, state.persona)
    assertEquals(4, state.streakDays)
    assertEquals(12, state.savesTotal)
    assertEquals(3, state.savesToday)
  }
}

private class FakeZenStatusProvider(
    private val accessibilityEnabled: Boolean,
    private val usageAccessEnabled: Boolean,
    private val stats: List<AppUsageItem>
) : ZenStatusProvider {
    override fun isAccessibilityEnabled(): Boolean = accessibilityEnabled
    override fun isUsageAccessEnabled(): Boolean = usageAccessEnabled
    override fun getDetailedUsageStats(): List<AppUsageItem> = stats
}

private class FakeStatsSource(
    override val persona: Persona = Persona.GOBLIN,
    override val streakDays: Int = 0,
    override val savesTotal: Int = 0,
    private val savesToday: Int = 0,
    override val dailyCapMinutes: Int = 60,
    override val earnedScrollsEnabled: Boolean = false,
    override val earnedBalanceSeconds: Int = 0
) : ZenStatsSource {
    override fun savesToday(): Int = savesToday
    override fun touchActiveDay() {}
}

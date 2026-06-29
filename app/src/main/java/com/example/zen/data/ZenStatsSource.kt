package com.example.zen.data

import com.example.zen.persona.Persona

/**
 * The persona + stats the dashboard reads. Implemented by [ZenPrefs]; abstracted so the
 * ViewModel can be unit-tested without an Android [android.content.Context].
 */
interface ZenStatsSource {
    val persona: Persona
    val streakDays: Int
    val savesTotal: Int
    val dailyCapMinutes: Int
    val earnedScrollsEnabled: Boolean
    val earnedBalanceSeconds: Int
    fun savesToday(): Int
    fun touchActiveDay()
}

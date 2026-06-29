package com.example.zen.data

import android.content.Context
import android.content.SharedPreferences
import com.example.zen.persona.Persona
import java.util.Calendar
import java.util.Locale

/**
 * Synchronous, on-device storage for all settings, the commitment lock, and stats.
 *
 * Backed by [SharedPreferences] (not DataStore) on purpose: the Accessibility Service must read
 * settings synchronously inside `onAccessibilityEvent`, and SharedPreferences gives fast blocking
 * reads. [revision] bumps on every change (including cross-process writes from the service) so the
 * UI can refresh.
 */
class ZenPrefs(context: Context) : ZenStatsSource {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Increments whenever any value changes; observe to refresh UI. */
    @Volatile
    var revision: Int = 0
        private set

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, _ -> revision++ }
    }

    // ---- Settings ------------------------------------------------------------------------------

    override var persona: Persona
        get() = Persona.fromId(prefs.getString(KEY_PERSONA, Persona.DEFAULT.id))
        set(value) = prefs.edit().putString(KEY_PERSONA, value.id).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** Packages the user has chosen to guard. Defaults to all known short-form apps. */
    var blockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED, DEFAULT_BLOCKED) ?: DEFAULT_BLOCKED
        set(value) = prefs.edit().putStringSet(KEY_BLOCKED, value).apply()

    var friendPassEnabled: Boolean
        get() = prefs.getBoolean(KEY_FRIEND_PASS, true)
        set(value) = prefs.edit().putBoolean(KEY_FRIEND_PASS, value).apply()

    /** Scrolls allowed on direct (non-friend-pass) entry before blocking. 0 = block on entry. */
    var allowedScrolls: Int
        get() = prefs.getInt(KEY_ALLOWED_SCROLLS, 0)
        set(value) = prefs.edit().putInt(KEY_ALLOWED_SCROLLS, value.coerceIn(0, 10)).apply()

    override var dailyCapMinutes: Int
        get() = prefs.getInt(KEY_DAILY_CAP, 60)
        set(value) = prefs.edit().putInt(KEY_DAILY_CAP, value.coerceIn(5, 600)).apply()

    override var earnedScrollsEnabled: Boolean
        get() = prefs.getBoolean(KEY_EARNED_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_EARNED_ENABLED, value).apply()

    override var earnedBalanceSeconds: Int
        get() = prefs.getInt(KEY_EARNED_BALANCE, 0)
        set(value) = prefs.edit().putInt(KEY_EARNED_BALANCE, value.coerceAtLeast(0)).apply()

    // ---- Commitment lock -----------------------------------------------------------------------
    // Stored in plaintext on purpose: this is a self-discipline lock, not security. The user can
    // always retrieve the password after the cooldown, so it must be readable.

    var lockPassword: String
        get() = prefs.getString(KEY_LOCK_PW, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOCK_PW, value).apply()

    fun hasPassword(): Boolean = lockPassword.isNotEmpty()

    private var unlockUntil: Long
        get() = prefs.getLong(KEY_UNLOCK_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_UNLOCK_UNTIL, value).apply()

    /** Timestamp the cooldown unlock becomes available, or 0 if no cooldown is pending. */
    private var pendingUnlockAt: Long
        get() = prefs.getLong(KEY_PENDING_UNLOCK, 0L)
        set(value) = prefs.edit().putLong(KEY_PENDING_UNLOCK, value).apply()

    fun isUnlocked(): Boolean = System.currentTimeMillis() < unlockUntil

    /** Returns true and opens the edit window if [attempt] matches the password. */
    fun tryPassword(attempt: String): Boolean {
        if (attempt == lockPassword && lockPassword.isNotEmpty()) {
            unlockUntil = System.currentTimeMillis() + UNLOCK_WINDOW_MS
            pendingUnlockAt = 0L
            return true
        }
        return false
    }

    /** Start the ~2-minute cooldown unlock (used when there's no password, or it's forgotten). */
    fun beginCooldown() {
        if (pendingUnlockAt == 0L) {
            pendingUnlockAt = System.currentTimeMillis() + COOLDOWN_MS
        }
    }

    fun cancelCooldown() {
        pendingUnlockAt = 0L
    }

    /** Millis left on the pending cooldown, or 0 if none / ready. */
    fun cooldownRemainingMs(): Long {
        val at = pendingUnlockAt
        if (at == 0L) return 0L
        return (at - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun isCooldownPending(): Boolean = pendingUnlockAt != 0L && cooldownRemainingMs() > 0L

    /** If a cooldown has elapsed, open the edit window. Returns true if newly unlocked. */
    fun completeCooldownIfReady(): Boolean {
        val at = pendingUnlockAt
        if (at != 0L && System.currentTimeMillis() >= at) {
            unlockUntil = System.currentTimeMillis() + UNLOCK_WINDOW_MS
            pendingUnlockAt = 0L
            return true
        }
        return false
    }

    fun lockNow() {
        unlockUntil = 0L
        pendingUnlockAt = 0L
    }

    // ---- Stats ---------------------------------------------------------------------------------

    override var savesTotal: Int
        get() = prefs.getInt(KEY_SAVES_TOTAL, 0)
        private set(value) = prefs.edit().putInt(KEY_SAVES_TOTAL, value).apply()

    /** Saves (interceptions) recorded today, after rolling over the day if needed. */
    override fun savesToday(): Int {
        rolloverDayIfNeeded()
        return prefs.getInt(KEY_SAVES_TODAY, 0)
    }

    override var streakDays: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        private set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    /**
     * Record an interception. Returns the relapse count *before* this one (so the first block of
     * the day is tier 0), used to escalate the persona's tone.
     */
    fun recordSave(): Int {
        rolloverDayIfNeeded()
        val before = prefs.getInt(KEY_SAVES_TODAY, 0)
        prefs.edit()
            .putInt(KEY_SAVES_TODAY, before + 1)
            .putInt(KEY_SAVES_TOTAL, savesTotal + 1)
            .apply()
        return before
    }

    /** Call when the user opens the app; advances the "days protected" streak. */
    override fun touchActiveDay() {
        val today = dayKey(0)
        val last = prefs.getString(KEY_LAST_ACTIVE, "") ?: ""
        if (last == today) return
        streakDays = if (last == dayKey(-1)) streakDays + 1 else 1
        prefs.edit().putString(KEY_LAST_ACTIVE, today).apply()
    }

    private fun rolloverDayIfNeeded() {
        val today = dayKey(0)
        val savesDay = prefs.getString(KEY_SAVES_DAY, "") ?: ""
        if (savesDay != today) {
            prefs.edit()
                .putString(KEY_SAVES_DAY, today)
                .putInt(KEY_SAVES_TODAY, 0)
                .apply()
        }
    }

    private fun dayKey(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        const val FILE = "zen_prefs"
        const val PASSWORD_LENGTH = 15
        private const val COOLDOWN_MS = 2 * 60 * 1000L
        private const val UNLOCK_WINDOW_MS = 5 * 60 * 1000L

        /** Known short-form-capable apps, guarded by default. */
        val DEFAULT_BLOCKED: Set<String> = KnownApps.allPackages

        private const val KEY_PERSONA = "persona"
        private const val KEY_ONBOARDED = "onboarding_complete"
        private const val KEY_BLOCKED = "blocked_packages"
        private const val KEY_FRIEND_PASS = "friend_pass_enabled"
        private const val KEY_ALLOWED_SCROLLS = "allowed_scrolls"
        private const val KEY_DAILY_CAP = "daily_cap_minutes"
        private const val KEY_EARNED_ENABLED = "earned_scrolls_enabled"
        private const val KEY_EARNED_BALANCE = "earned_balance_seconds"
        private const val KEY_LOCK_PW = "lock_password"
        private const val KEY_UNLOCK_UNTIL = "unlock_until"
        private const val KEY_PENDING_UNLOCK = "pending_unlock_at"
        private const val KEY_SAVES_TOTAL = "saves_total"
        private const val KEY_SAVES_TODAY = "saves_today"
        private const val KEY_SAVES_DAY = "saves_day"
        private const val KEY_STREAK = "streak_days"
        private const val KEY_LAST_ACTIVE = "last_active_day"
    }
}

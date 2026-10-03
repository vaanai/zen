package com.example.zen.data

/**
 * The three rules the accessibility service already keeps.
 * Each one is a [friendPassEnabled] and [allowedScrolls] pair. Nothing else.
 */
enum class GuardMode {
    FRIENDS_OPEN,
    A_FEW_SCROLLS,
    ALL_STOPS;

    fun toRule(feedScrolls: Int = FEW_SCROLLS_DEFAULT): KeptRule = when (this) {
        FRIENDS_OPEN -> KeptRule(friendPassEnabled = true, allowedScrolls = 0)
        ALL_STOPS -> KeptRule(friendPassEnabled = false, allowedScrolls = 0)
        A_FEW_SCROLLS -> KeptRule(
            friendPassEnabled = true,
            allowedScrolls = feedScrolls.coerceIn(FEW_SCROLLS_MIN, FEW_SCROLLS_MAX)
        )
    }

    companion object {
        const val FEW_SCROLLS_MIN = 1
        const val FEW_SCROLLS_MAX = 3
        const val FEW_SCROLLS_DEFAULT = 1

        /** Null when the stored prefs are a leftover the picker does not offer. */
        fun fromPrefs(friendPassEnabled: Boolean, allowedScrolls: Int): GuardMode? {
            val scrolls = allowedScrolls.coerceAtLeast(0)
            return when {
                friendPassEnabled && scrolls == 0 -> FRIENDS_OPEN
                friendPassEnabled && scrolls in FEW_SCROLLS_MIN..FEW_SCROLLS_MAX -> A_FEW_SCROLLS
                !friendPassEnabled && scrolls == 0 -> ALL_STOPS
                else -> null
            }
        }

        /**
         * A pair the picker can write. A leftover allowance collapses to [FRIENDS_OPEN]
         * so onboarding cannot save a rule the modes do not show.
         */
        fun kept(friendPassEnabled: Boolean, allowedScrolls: Int): KeptRule {
            val mode = fromPrefs(friendPassEnabled, allowedScrolls) ?: FRIENDS_OPEN
            return mode.toRule(allowedScrolls)
        }
    }
}

data class KeptRule(
    val friendPassEnabled: Boolean,
    val allowedScrolls: Int
)

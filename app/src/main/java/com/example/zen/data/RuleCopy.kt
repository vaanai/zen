package com.example.zen.data

/**
 * The rule [com.example.zen.ShortFormSession] keeps, said once so every screen can use it.
 *
 * Friend Pass on: an armed viewing session stays open. A feed opened with no pass stops,
 * or lasts for [allowedScrolls] when that allowance is above zero. A suggestion in the
 * same viewer cannot be told from a friend, and it still plays.
 *
 * TikTok is short-form for the whole package, so opening it stops.
 * YouTube has no messages inside this app; a Short stays open only when another app just opened it.
 */
object RuleCopy {

    const val TIKTOK =
        "Opening TikTok stops. Zen can't tell a friend's video from For You here."

    const val YOUTUBE =
        "YouTube Shorts stay open only when a message in another app just opened them. Zen can't see a YouTube message."

    private const val SUGGESTION =
        "A suggestion in the same viewer can't be told from a friend, so it can still play."

    fun sentence(friendPassEnabled: Boolean, allowedScrolls: Int): String {
        val scrolls = allowedScrolls.coerceAtLeast(0)
        return if (friendPassEnabled) {
            "Friends' reels stay open. ${feedClause(scrolls)} $SUGGESTION"
        } else if (scrolls == 0) {
            "Short-form stops, including videos from friends."
        } else {
            "Short-form stops after ${scrollCount(scrolls)}, including videos from friends."
        }
    }

    /** What an unarmed open does. The slider is this, not a count of friend reels. */
    fun feedClause(allowedScrolls: Int): String {
        val scrolls = allowedScrolls.coerceAtLeast(0)
        return if (scrolls == 0) {
            "A feed you open yourself stops."
        } else {
            "A feed you open yourself gets ${scrollCount(scrolls)}, then it stops."
        }
    }

    /**
     * Limits that do not use [sentence]. TikTok never gets the friends-open line.
     * YouTube's line is only true while Friend Pass is on.
     */
    fun limits(
        friendPassEnabled: Boolean,
        tiktokGuarded: Boolean,
        youtubeGuarded: Boolean
    ): List<String> = buildList {
        if (tiktokGuarded) add(TIKTOK)
        if (youtubeGuarded) appLimit("YouTube", friendPassEnabled)?.let { add(it) }
    }

    /**
     * The rule an in-app accessibility row may state for this selection.
     * Includes the suggestion hole, and TikTok or YouTube, when those limits apply.
     */
    fun stated(
        friendPassEnabled: Boolean,
        allowedScrolls: Int,
        tiktokGuarded: Boolean,
        youtubeGuarded: Boolean
    ): String = buildList {
        add(sentence(friendPassEnabled, allowedScrolls))
        addAll(limits(friendPassEnabled, tiktokGuarded, youtubeGuarded))
    }.joinToString(" ")

    /** The line for one app row, or null when that app uses the main sentence. */
    fun appLimit(appName: String, friendPassEnabled: Boolean): String? = when (appName) {
        "TikTok" -> TIKTOK
        "YouTube" -> if (friendPassEnabled) YOUTUBE else null
        else -> null
    }

    private fun scrollCount(n: Int): String = if (n == 1) "1 scroll" else "$n scrolls"
}

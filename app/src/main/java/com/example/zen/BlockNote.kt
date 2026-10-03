package com.example.zen

/**
 * The confirmation a block speaks.
 *
 * [ZenAccessibilityService] shows this only after a decision to block. An armed friend
 * session never does. The words are the short noun — not a persona roast, and not the
 * longer sentence on the home screen.
 */
object BlockNote {
    const val LINE = "That's the feed."

    /**
     * How long the note stays. Auto-dismiss is the path that has to work; a tap may
     * remove it sooner. Under a second, so the social app remains the app.
     */
    const val DISMISS_AFTER_MS = 800L
}

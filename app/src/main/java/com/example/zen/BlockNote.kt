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
     * How long the note stays. The timer removes it. A tap is not part of that,
     * because the tap belongs to the social app.
     */
    const val DISMISS_AFTER_MS = 800L

    /** The note is drawn over the app and does not take the next touch. */
    const val PASSES_TOUCHES = true
}

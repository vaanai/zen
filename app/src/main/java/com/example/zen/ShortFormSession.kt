package com.example.zen

/**
 * Short-form allow/block decision, extracted so it can run without an accessibility service.
 *
 * Friend Pass is armed once: a viewer entry that follows a person-surface (an in-app DM, or an
 * external messenger) within [FRIEND_PASS_WINDOW_MS]. After that the pass is the session, not the
 * clock. Scrolls inside it are not counted and do not block. A transient `isShortForm == false`
 * (null root, a missed frame, comments) does not end it and does not re-read the clock.
 *
 * [onBlocked] ends the feed that was just intercepted and keeps the pass so the next viewer entry
 * restores it — including when Back lands on a non-player first. [onLeftGuardedApp] ends the live
 * session (the user left the guarded app). A consumed arm is not reused after that, and an
 * external messenger event does not by itself start the session again. It can still arm the next
 * viewer entry, which is how a link shared in another app opens.
 *
 * An unarmed entry with allowance 0 still blocks on landing. The allowance is not how a friend
 * session stays open.
 */
class ShortFormSession {

    data class Settings(
        val friendPassEnabled: Boolean,
        val allowedScrolls: Int,
        val earnedScrollsEnabled: Boolean = false
    )

    data class Decision(
        val block: Boolean,
        val armed: Boolean
    )

    /** Scrolls counted against the unarmed-feed allowance. Friend-session scrolls are not included. */
    val countedScrolls: Int
        get() = scrolls

    /** True while a friend viewing session is live. False after a leave or a block, until restore. */
    val isFriendSession: Boolean
        get() = friendPackage != null

    private var pendingArmAt: Long? = null
    private var friendPackage: String? = null
    private var rememberedFriendPackage: String? = null
    private var unarmedPackage: String? = null
    private var restoreOnNextEnter = false
    private var heldFromFriend = false
    private var scrolls = 0

    /**
     * In-app chat surface. Queues the next new entry only. A live friend session does not read it,
     * so a keyword on the reel cannot extend that session.
     */
    fun noteInAppPersonSurface(now: Long) {
        pendingArmAt = now
    }

    /**
     * External messenger. Records the next viewer entry only; it does not start, extend, or
     * resurrect a session.
     */
    fun noteExternalMessenger(now: Long) {
        pendingArmAt = now
    }

    /** User left the guarded apps. The live session ends. A one-shot restore after [onBlocked] stays. */
    fun onLeftGuardedApp() {
        friendPackage = null
        rememberedFriendPackage = null
        unarmedPackage = null
        heldFromFriend = false
        scrolls = 0
    }

    /**
     * Zen intercepted a feed and pressed Back. If a friend pass was live or held, the next viewer
     * entry restores it instead of taking the direct-entry block.
     */
    fun onBlocked() {
        if (friendPackage != null || rememberedFriendPackage != null || heldFromFriend || restoreOnNextEnter) {
            restoreOnNextEnter = true
        }
        friendPackage = null
        rememberedFriendPackage = null
        heldFromFriend = false
        unarmedPackage = null
        scrolls = 0
    }

    fun onViewer(
        visible: Boolean,
        packageName: String,
        now: Long,
        settings: Settings
    ): Decision {
        applyEnabled(settings)
        if (!visible) {
            if (friendPackage == null) {
                unarmedPackage = null
                scrolls = 0
            }
            return Decision(block = false, armed = friendPackage != null)
        }
        return enter(packageName, now, settings)
    }

    fun onScroll(
        visible: Boolean,
        packageName: String,
        now: Long,
        settings: Settings
    ): Decision {
        applyEnabled(settings)
        if (!visible) {
            return onViewer(visible = false, packageName, now, settings)
        }
        if (friendPackage == packageName) {
            return Decision(block = false, armed = true)
        }
        val needsEntry = restoreOnNextEnter || friendPackage != null || unarmedPackage != packageName
        if (needsEntry) {
            val entered = enter(packageName, now, settings)
            if (entered.block || entered.armed) return entered
        }
        scrolls += 1
        if (scrolls > allowance(settings)) {
            return Decision(block = true, armed = false)
        }
        return Decision(block = false, armed = false)
    }

    private fun enter(packageName: String, now: Long, settings: Settings): Decision {
        if (settings.friendPassEnabled && restoreOnNextEnter) {
            return resumeFriend(packageName)
        }
        if (friendPackage == packageName) {
            return Decision(block = false, armed = true)
        }
        // Same unarmed feed, including one opened while a friend pass is held for later.
        // Content events must not reset its scroll count or re-read the arm clock.
        if (unarmedPackage == packageName && friendPackage == null) {
            return Decision(block = false, armed = false)
        }

        val keepFriend = friendPackage != null || heldFromFriend
        if (keepFriend && settings.friendPassEnabled && packageName == rememberedFriendPackage) {
            return resumeFriend(packageName)
        }

        val arm = settings.friendPassEnabled &&
            pendingArmAt?.let { now - it < FRIEND_PASS_WINDOW_MS } == true
        pendingArmAt = null

        if (arm) {
            return resumeFriend(packageName)
        }

        friendPackage = null
        unarmedPackage = packageName
        scrolls = 0
        if (allowance(settings) == 0) {
            if (keepFriend && settings.friendPassEnabled) {
                restoreOnNextEnter = true
            }
            heldFromFriend = false
            unarmedPackage = null
            return Decision(block = true, armed = false)
        }
        heldFromFriend = keepFriend && settings.friendPassEnabled
        return Decision(block = false, armed = false)
    }

    private fun resumeFriend(packageName: String): Decision {
        restoreOnNextEnter = false
        heldFromFriend = false
        friendPackage = packageName
        rememberedFriendPackage = packageName
        unarmedPackage = null
        scrolls = 0
        return Decision(block = false, armed = true)
    }

    private fun applyEnabled(settings: Settings) {
        if (!settings.friendPassEnabled) {
            friendPackage = null
            rememberedFriendPackage = null
            restoreOnNextEnter = false
            heldFromFriend = false
        }
    }

    private fun allowance(settings: Settings): Int {
        val base = settings.allowedScrolls
        return if (settings.earnedScrollsEnabled) base + 1 else base
    }

    companion object {
        /** How long after a person-surface the *next* viewer entry can arm. Not a session lifetime. */
        const val FRIEND_PASS_WINDOW_MS = 4_000L
    }
}

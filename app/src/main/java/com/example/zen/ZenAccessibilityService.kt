package com.example.zen

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LineLibrary

/**
 * Core engine. Detects when the user is on a short-form feed in a guarded app and intercepts
 * doom-scrolling.
 *
 * Detection is behavioural (scroll count within a feed session) rather than fragile screen
 * fingerprinting:
 *  - **Direct entry** (you opened the feed yourself): block on entry, unless the user has configured
 *    a scroll allowance.
 *  - **Friend Pass** (you arrived from a DM within [FRIEND_PASS_WINDOW_MS]): the landed video is
 *    allowed; the moment you scroll to the next one, you're intercepted.
 */
class ZenAccessibilityService : AccessibilityService() {

    private val TAG = "ZenBlocker"

    private lateinit var prefs: ZenPrefs
    private var overlay: InterceptionOverlay? = null

    private val messagingPackages = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.facebook.orca",
        "com.discord",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )
    private val tiktokPackages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

    private var lastDirectMessageTime = 0L
    private var lastBlockTime = 0L

    // Current short-form "session" state.
    private var sessionPackage: String? = null
    private var sessionScrolls = 0
    private var sessionFriendPass = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = ZenPrefs(applicationContext)
        overlay = InterceptionOverlay(this)
        Log.d(TAG, "Zen service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName in messagingPackages) {
            lastDirectMessageTime = System.currentTimeMillis()
        }

        val guarded = prefs.blockedPackages
        if (packageName !in guarded) {
            resetSession()
            return
        }

        if (System.currentTimeMillis() - lastBlockTime < BLOCK_COOLDOWN_MS) return

        val root = rootInActiveWindow

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(packageName, root)

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (root != null && isDirectMessageScreen(root, packageName)) {
                    lastDirectMessageTime = System.currentTimeMillis()
                }
                if (isShortForm(packageName, root)) {
                    enterShortForm(packageName)
                } else {
                    resetSession()
                }
            }
        }
    }

    private fun handleScroll(packageName: String, root: AccessibilityNodeInfo?) {
        if (!isShortForm(packageName, root)) {
            resetSession()
            return
        }
        enterShortForm(packageName)
        sessionScrolls++
        if (sessionScrolls > currentAllowance()) {
            block(packageName)
        }
    }

    /** Begin a feed session (idempotent for the same package). */
    private fun enterShortForm(packageName: String) {
        if (sessionPackage == packageName) return
        sessionPackage = packageName
        sessionScrolls = 0
        sessionFriendPass = prefs.friendPassEnabled &&
            (System.currentTimeMillis() - lastDirectMessageTime < FRIEND_PASS_WINDOW_MS)

        // Direct entry with no scroll allowance: block immediately on landing in the feed.
        if (!sessionFriendPass && currentAllowance() == 0) {
            block(packageName)
        }
    }

    /** Scrolls permitted before blocking. Friend Pass = block on first scroll past the landed video. */
    private fun currentAllowance(): Int {
        if (sessionFriendPass) return 0
        val base = prefs.allowedScrolls
        // Optional "earned / lenient" mode grants a little extra rope.
        return if (prefs.earnedScrollsEnabled) base + 1 else base
    }

    private fun block(packageName: String) {
        lastBlockTime = System.currentTimeMillis()
        val relapseTier = prefs.recordSave()
        val persona = prefs.persona
        val line = LineLibrary.blockLine(persona, relapseTier)
        Log.d(TAG, "Blocked $packageName (relapse #$relapseTier): $line")
        overlay?.show(persona, line)
        performGlobalAction(GLOBAL_ACTION_BACK)
        resetSession()
    }

    private fun resetSession() {
        sessionPackage = null
        sessionScrolls = 0
        sessionFriendPass = false
    }

    private fun isShortForm(packageName: String, root: AccessibilityNodeInfo?): Boolean {
        // TikTok is exclusively short-form.
        if (packageName in tiktokPackages) return true
        return root != null && containsShortFormIndicator(root, 0)
    }

    private fun containsShortFormIndicator(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 8) return false
        val indicators = listOf("reels", "shorts", "spotlight", "for you")
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()
        if (text != null && indicators.any { text.contains(it) }) return true
        if (desc != null && indicators.any { desc.contains(it) }) return true
        for (i in 0 until node.childCount) {
            if (containsShortFormIndicator(node.getChild(i), depth + 1)) return true
        }
        return false
    }

    private fun isDirectMessageScreen(node: AccessibilityNodeInfo?, packageName: String, depth: Int = 0): Boolean {
        if (node == null || depth > 8) return false
        val keywords = when (packageName) {
            "com.instagram.android" ->
                listOf("message...", "messages", "direct", "chats", "active now", "write a message")
            "com.snapchat.android" ->
                listOf("chat", "send a chat", "new chat", "friends")
            else -> return false
        }
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()
        if (text != null && keywords.any { text.contains(it) }) return true
        if (desc != null && keywords.any { desc.contains(it) }) return true
        for (i in 0 until node.childCount) {
            if (isDirectMessageScreen(node.getChild(i), packageName, depth + 1)) return true
        }
        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Zen service interrupted")
    }

    companion object {
        private const val BLOCK_COOLDOWN_MS = 1500L
        private const val FRIEND_PASS_WINDOW_MS = 4000L
    }
}

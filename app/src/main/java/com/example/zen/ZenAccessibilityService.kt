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

    private var lastDumpTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = ZenPrefs(applicationContext)
        overlay = InterceptionOverlay(this)
        Log.d(TAG, "Zen service connected. Guarding: ${prefs.blockedPackages}")
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
                // Diagnostic: dump what this app actually exposes so we can tune detection from a
                // real logcat (`adb logcat -s ZenScan`). Throttled so it doesn't flood.
                maybeDumpTree(packageName, root)

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
        Log.d(TAG, "Entered short-form feed in $packageName (friendPass=$sessionFriendPass, allowance=${currentAllowance()})")

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
        if (root == null) return false
        return treeAnyMatch(root) { node -> matchesShortForm(packageName, node) }
    }

    /**
     * Whether a single node identifies the *active short-form player* (not merely a nav tab).
     *
     * Resource-ids are the reliable discriminator: the "Reels"/"Shorts" bottom-nav tabs are present
     * on every screen (including the home feed), so matching on the words alone would false-positive
     * everywhere. The reel/short *viewer* exposes distinctive container ids instead. Text is only a
     * last-resort fallback for apps whose ids are fully obfuscated (e.g. Snapchat Spotlight).
     */
    private fun matchesShortForm(pkg: String, node: AccessibilityNodeInfo): Boolean {
        val id = node.viewIdResourceName?.lowercase()
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()
        return when (pkg) {
            "com.instagram.android" ->
                idContains(id, "clips_viewer", "clips_video", "reel_viewer", "reel_feed")
            "com.google.android.youtube" ->
                idContains(id, "reel_recycler", "reel_player", "shorts_player", "reel_watch")
            "com.snapchat.android" ->
                idContains(id, "spotlight", "discover_feed") || anyContains(text, desc, "spotlight")
            else -> anyContains(text, desc, "reels", "shorts", "spotlight", "for you")
        }
    }

    private fun idContains(id: String?, vararg needles: String): Boolean =
        id != null && needles.any { id.contains(it) }

    private fun anyContains(text: String?, desc: String?, vararg needles: String): Boolean =
        (text != null && needles.any { text.contains(it) }) ||
            (desc != null && needles.any { desc.contains(it) })

    /** Depth-bounded, node-count-bounded full-tree search that short-circuits on the first match. */
    private fun treeAnyMatch(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): Boolean {
        var visited = 0
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            if (visited++ > MAX_NODES) break
            if (predicate(node)) return true
            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    stack.addLast(child to depth + 1)
                }
            }
        }
        return false
    }

    /**
     * Logs the resource-ids / text / content-descriptions the current screen exposes, throttled to
     * once per [DUMP_THROTTLE_MS]. This is how we learn each app's *real* ids when testing on-device:
     * `adb logcat -s ZenScan`. Only nodes carrying an id or visible text are logged, capped in count.
     */
    private fun maybeDumpTree(packageName: String, root: AccessibilityNodeInfo?) {
        if (root == null) return
        val now = System.currentTimeMillis()
        if (now - lastDumpTime < DUMP_THROTTLE_MS) return
        lastDumpTime = now

        Log.d(SCAN_TAG, "--- window in $packageName (shortForm=${isShortForm(packageName, root)}) ---")
        var visited = 0
        var logged = 0
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && logged < MAX_DUMP_LINES) {
            val (node, depth) = stack.removeLast()
            if (visited++ > MAX_NODES) break
            val id = node.viewIdResourceName
            val text = node.text?.toString()?.takeIf { it.isNotBlank() }
            val desc = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
            if (id != null || text != null || desc != null) {
                Log.d(SCAN_TAG, "id=$id text=$text desc=$desc")
                logged++
            }
            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    stack.addLast(child to depth + 1)
                }
            }
        }
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
        private const val SCAN_TAG = "ZenScan"
        private const val BLOCK_COOLDOWN_MS = 1500L
        private const val FRIEND_PASS_WINDOW_MS = 4000L
        private const val MAX_DEPTH = 30
        private const val MAX_NODES = 2000
        private const val DUMP_THROTTLE_MS = 2000L
        private const val MAX_DUMP_LINES = 60
    }
}

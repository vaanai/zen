package com.example.zen

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.zen.data.ZenPrefs

/**
 * Core engine. Detects when the user is on a short-form feed in a guarded app and intercepts
 * an unarmed feed.
 *
 * The allow/block decision lives in [ShortFormSession]:
 *  - **Friend session** — armed once from a person-surface, then kept across scrolls, transient
 *    misses, and Zen's own Back. It is not recomputed from the clock.
 *  - **Direct entry** — no armed pass. Block on landing when the scroll allowance is 0.
 */
class ZenAccessibilityService : AccessibilityService() {

    private val TAG = "ZenBlocker"

    private lateinit var prefs: ZenPrefs
    private var overlay: InterceptionOverlay? = null
    private val session = ShortFormSession()

    private val messagingPackages = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.facebook.orca",
        "com.discord",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )
    private var armLogged = false
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
        val now = System.currentTimeMillis()

        if (packageName in messagingPackages) {
            session.noteExternalMessenger(now)
        }

        val guarded = prefs.blockedPackages
        if (packageName !in guarded) {
            // Leaving the guarded app ends the live session. A messenger event is only the next
            // entry's arm, and the shade / keyboard are not a leave.
            if (isRealLeave(packageName, event.eventType, guarded)) {
                session.onLeftGuardedApp()
            }
            return
        }

        val settings = currentSettings()
        // The active window is ours to recycle, including when this event is neither a scroll nor a window.
        // A null root is a missed frame. It must not look like Home, or the one-block latch clears
        // and the same viewer fires Back again.
        val root = obtainActiveWindow()
        val decision = useObtainedOrNull(root, FrameworkNode::recycle) { window ->
            if (window == null) return
            val reading = ScreenClass.read(packageName, window)
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                    session.onScroll(
                        reading.shortForm,
                        packageName,
                        now,
                        settings,
                        clearsLatch = reading.clearsLatch,
                    )

                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    // Diagnostic: dump what this app actually exposes so we can tune detection from a
                    // real logcat (`adb logcat -s ZenScan`). Throttled so it doesn't flood.
                    maybeDumpTree(packageName, window, reading.shortForm)

                    if (reading.personSurface) session.noteInAppPersonSurface(now)
                    session.onViewer(
                        reading.shortForm,
                        packageName,
                        now,
                        settings,
                        clearsLatch = reading.clearsLatch,
                    )
                }

                else -> return
            }
        }

        if (decision.armed && !armLogged) {
            Log.d(TAG, "Friend session open in $packageName")
            armLogged = true
        }
        if (!decision.armed) armLogged = false
        if (decision.block) block(packageName)
    }

    /**
     * Home (or any other non-guarded app) is in front, and this event is that window opening.
     * Messenger packages arm the next entry instead. System UI and the keyboard are not an exit.
     */
    private fun isRealLeave(packageName: String, eventType: Int, guarded: Set<String>): Boolean {
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return false
        if (packageName in messagingPackages || packageName == SYSTEM_UI_PACKAGE) return false
        if (packageName.contains("inputmethod")) return false
        val root = obtainActiveWindow() ?: return false
        return useObtained(root, FrameworkNode::recycle) { window ->
            val foreground = window.packageName?.toString() ?: return@useObtained false
            foreground == packageName && foreground !in guarded
        }
    }

    private fun currentSettings() = ShortFormSession.Settings(
        friendPassEnabled = prefs.friendPassEnabled,
        allowedScrolls = prefs.allowedScrolls,
        earnedScrollsEnabled = prefs.earnedScrollsEnabled
    )

    private fun block(packageName: String) {
        val relapseTier = prefs.recordSave()
        Log.d(TAG, "Blocked $packageName (relapse #$relapseTier): ${BlockNote.LINE}")
        overlay?.show(prefs.persona)
        performGlobalAction(GLOBAL_ACTION_BACK)
        // Back must not forget an armed pass. The next viewer entry restores it.
        session.onBlocked()
    }

    /**
     * Logs the resource-ids / text / content-descriptions the current screen exposes, throttled to
     * once per [DUMP_THROTTLE_MS]. This is how we learn each app's *real* ids when testing on-device:
     * `adb logcat -s ZenScan`. Only nodes carrying an id or visible text are logged, capped in count.
     */
    private fun maybeDumpTree(packageName: String, root: WalkNode, shortForm: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastDumpTime < DUMP_THROTTLE_MS) return
        lastDumpTime = now

        Log.d(SCAN_TAG, "--- window in $packageName (shortForm=$shortForm) ---")
        var logged = 0
        NodeWalk.walk(
            root,
            maxNodes = MAX_NODES,
            maxDepth = MAX_DEPTH,
            stopBeforeVisit = { logged >= MAX_DUMP_LINES },
        ) { node ->
            val id = node.viewIdResourceName
            val text = node.text?.toString()?.takeIf { it.isNotBlank() }
            val desc = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
            if (id != null || text != null || desc != null) {
                Log.d(SCAN_TAG, "id=$id text=$text desc=$desc")
                logged++
            }
            false
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Zen service interrupted")
    }

    companion object {
        private const val SCAN_TAG = "ZenScan"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val MAX_DEPTH = 30
        private const val MAX_NODES = 2000
        private const val DUMP_THROTTLE_MS = 2000L
        private const val MAX_DUMP_LINES = 60
    }
}

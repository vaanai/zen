package com.example.zen

/**
 * What the guarded window is, separate from whether that should block.
 *
 * Instagram keeps `clips_viewer_view_pager` in the hierarchy on Home. After a recent
 * Instagram build that node is reported visible at 0×0. A substring match on `clips_viewer`
 * (and on `clips_video`, which is an ordinary post) therefore called the home window a
 * viewer. With allowance 0, [ShortFormSession] blocked on landing: the note covered the
 * screen and Back ran against Home, which does not navigate away, so the next content
 * event blocked again. The Messages tab never received the tap.
 *
 * A Reels / Shorts / Spotlight / Messages label on the chrome is not a viewer and not a
 * person surface. Home, the inbox, a DM, and a normal post are not short-form. The player
 * is a viewer only when its own id is on screen and the selected tab is not Home.
 */
internal object ScreenClass {

    data class Reading(
        val shortForm: Boolean,
        val personSurface: Boolean,
    )

    fun read(packageName: String, root: WalkNode?): Reading {
        if (packageName in TIKTOK) return Reading(shortForm = true, personSurface = false)
        if (root == null) return Reading(shortForm = false, personSurface = false)
        val seen = Seen()
        NodeWalk.walk(root, MAX_NODES, MAX_DEPTH) { node ->
            seen.take(packageName, node)
            false
        }
        return seen.reading()
    }

    private class Seen {
        private var viewerOnScreen = false
        private var homeTabSelected = false
        private var personOnScreen = false

        fun take(packageName: String, node: WalkNode) {
            val suffix = idSuffix(node.viewIdResourceName)
            val onScreen = node.visibleToUser && node.width > 0 && node.height > 0
            when (packageName) {
                INSTAGRAM -> {
                    if (onScreen && suffix == HOME_TAB && node.selected) homeTabSelected = true
                    if (onScreen && suffix in INSTAGRAM_VIEWER) viewerOnScreen = true
                    if (onScreen && instagramPerson(suffix, node)) personOnScreen = true
                }
                YOUTUBE -> {
                    if (onScreen && suffix in YOUTUBE_VIEWER) viewerOnScreen = true
                }
                SNAPCHAT -> {
                    if (onScreen && snapViewer(suffix)) viewerOnScreen = true
                    if (onScreen && snapPerson(suffix, node)) personOnScreen = true
                }
            }
        }

        fun reading(): Reading = Reading(
            // Home and an open chat win over a viewer id sitting in the same window.
            shortForm = viewerOnScreen && !homeTabSelected && !personOnScreen,
            personSurface = personOnScreen,
        )
    }

    private fun instagramPerson(suffix: String?, node: WalkNode): Boolean {
        if (suffix in CHROME_TABS) return false
        if (suffix != null && PERSON_ID_PARTS.any { suffix.contains(it) }) return true
        val blob = blob(node)
        return blob.isNotEmpty() && PERSON_PHRASES.any { blob.contains(it) }
    }

    private fun snapViewer(suffix: String?): Boolean {
        if (suffix == null || !suffix.contains("spotlight")) return false
        return CHROME_WORDS.none { suffix.contains(it) }
    }

    private fun snapPerson(suffix: String?, node: WalkNode): Boolean {
        if (suffix != null && CHROME_WORDS.any { suffix.contains(it) }) return false
        val blob = blob(node)
        return blob.isNotEmpty() && SNAP_PHRASES.any { blob.contains(it) }
    }

    private fun blob(node: WalkNode): String {
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        return "$text $desc".trim()
    }

    private fun idSuffix(id: String?): String? {
        if (id.isNullOrBlank()) return null
        val slash = id.lastIndexOf('/')
        return id.substring(slash + 1).lowercase()
    }

    private val TIKTOK = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

    private const val INSTAGRAM = "com.instagram.android"
    private const val YOUTUBE = "com.google.android.youtube"
    private const val SNAPCHAT = "com.snapchat.android"
    private const val HOME_TAB = "feed_tab"

    /** The player. Not the tab, not a post, not the 0×0 pager left on Home. */
    private val INSTAGRAM_VIEWER = setOf(
        "clips_viewer_view_pager",
        "root_clips_layout",
    )

    /** The Shorts player. `reel_recycler` is the shelf on the YouTube home screen. */
    private val YOUTUBE_VIEWER = setOf(
        "reel_watch_fragment_root",
        "reel_player_page_container",
        "shorts_player",
        "reel_watch_player",
    )

    private val CHROME_TABS = setOf(
        "feed_tab",
        "clips_tab",
        "direct_tab",
        "profile_tab",
        "search_tab",
        "camera_tab",
        "news_tab",
    )

    private val PERSON_ID_PARTS = listOf(
        "inbox",
        "direct_thread",
        "thread_composer",
        "message_composer",
        "composer_content",
    )

    /** Composer copy. The word "messages" is the tab, and it is not one of these. */
    private val PERSON_PHRASES = listOf(
        "write a message",
        "message...",
    )

    private val SNAP_PHRASES = listOf(
        "send a chat",
        "new chat",
    )

    private val CHROME_WORDS = listOf("tab", "button", "nav", "icon", "tray", "label", "bar")

    private const val MAX_NODES = 2000
    private const val MAX_DEPTH = 30
}

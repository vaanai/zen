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
 *
 * YouTube's Shorts shelf, the word "Shorts", and a recycler are the same kind of miss:
 * they sit on Home. The Shorts player is the viewer. Snapchat's Spotlight tab label is
 * not a player. TikTok opens on the feed, so that landing may stop, once. A non-feed
 * surface in that tree is not a viewer. A block does not run again on the surface Back
 * returns to; that latch lives in [ShortFormSession]. A one-frame miss while the player
 * is still up does not clear it. YouTube or Snapchat chrome does not either, while the
 * player id is still in the window. The Shorts shelf and the Spotlight tab do, once
 * that id is gone. A TikTok line that only reads like a profile or a composer does not.
 * A null root does not. Home does.
 */
internal object ScreenClass {

    data class Reading(
        val shortForm: Boolean,
        val personSurface: Boolean,
        /** Home, the inbox, or another real destination. A miss while the player is up is false. */
        val clearsLatch: Boolean,
    )

    fun read(packageName: String, root: WalkNode?): Reading {
        // Opening TikTok is the feed, including a missed frame. A null root must not look
        // like a non-feed surface, or the one-block latch would clear and Back would fire again.
        val tiktok = packageName in TIKTOK
        if (root == null) return Reading(shortForm = tiktok, personSurface = false, clearsLatch = false)
        val seen = Seen()
        NodeWalk.walk(root, MAX_NODES, MAX_DEPTH) { node ->
            seen.take(packageName, node)
            false
        }
        return seen.reading(tiktok)
    }

    private class Seen {
        private var viewerOnScreen = false
        private var homeTabSelected = false
        private var personOnScreen = false
        private var navigatedAway = false
        private var playerIdInTree = false
        private var shelfOnScreen = false
        private var spotlightTabOnScreen = false
        private var tiktokNonFeed = false
        private var tiktokLeftFeed = false

        fun take(packageName: String, node: WalkNode) {
            val suffix = idSuffix(node.viewIdResourceName)
            val onScreen = node.visibleToUser && node.width > 0 && node.height > 0
            if (packageName in TIKTOK) {
                if (!onScreen) return
                // A profile or composer line can sit on the feed for one frame. It is not
                // the feed, and it is not a trip to Inbox.
                if (tiktokPhrase(node)) tiktokNonFeed = true
                if (tiktokStructuralNonFeed(suffix, node)) {
                    tiktokNonFeed = true
                    tiktokLeftFeed = true
                }
                return
            }
            when (packageName) {
                INSTAGRAM -> {
                    if (onScreen && suffix == HOME_TAB && node.selected) homeTabSelected = true
                    if (onScreen && suffix in INSTAGRAM_VIEWER) viewerOnScreen = true
                    if (onScreen && instagramPerson(suffix, node)) personOnScreen = true
                    if (onScreen && node.selected && suffix in DESTINATION_TABS) navigatedAway = true
                }
                YOUTUBE -> {
                    // The shelf stays up inside the player. It is an exit only once the
                    // player id is gone from the window.
                    if (youtubePlayer(suffix)) {
                        playerIdInTree = true
                        if (onScreen) viewerOnScreen = true
                    }
                    if (onScreen && youtubeShelf(suffix)) shelfOnScreen = true
                }
                SNAPCHAT -> {
                    // The Spotlight tab stays up with the player. It is an exit only once
                    // the player id is gone.
                    if (snapViewer(suffix)) {
                        playerIdInTree = true
                        if (onScreen) viewerOnScreen = true
                    }
                    if (onScreen && suffix == SPOTLIGHT_TAB) spotlightTabOnScreen = true
                    if (onScreen && snapPerson(suffix, node)) personOnScreen = true
                }
            }
        }

        fun reading(tiktok: Boolean): Reading {
            if (tiktok) {
                // The inbox does not arm a friend session. TikTok cannot tell a friend from For You.
                return Reading(
                    shortForm = !tiktokNonFeed,
                    personSurface = false,
                    clearsLatch = tiktokLeftFeed,
                )
            }
            val shortForm = viewerOnScreen && !homeTabSelected && !personOnScreen
            // Home wins over a sized pager. A sized player with no Home tab is still that player,
            // even when this frame also looks like a chat. Anything else needs a real destination.
            val clearsLatch = when {
                shortForm -> false
                homeTabSelected -> true
                viewerOnScreen -> false
                // Chrome around a player id that this frame cannot see is still that player.
                playerIdInTree -> false
                else -> navigatedAway || personOnScreen || shelfOnScreen || spotlightTabOnScreen
            }
            return Reading(
                shortForm = shortForm,
                personSurface = personOnScreen,
                clearsLatch = clearsLatch,
            )
        }
    }

    private fun instagramPerson(suffix: String?, node: WalkNode): Boolean {
        if (suffix in CHROME_TABS) return false
        if (suffix != null && PERSON_ID_PARTS.any { suffix.contains(it) }) return true
        val blob = blob(node)
        return blob.isNotEmpty() && PERSON_PHRASES.any { blob.contains(it) }
    }

    /**
     * The Shorts player. A shelf id and a recycler are on Home, including one whose name
     * also contains "player". The word "Shorts" is not an id and is not read here.
     */
    private fun youtubePlayer(suffix: String?): Boolean {
        if (suffix == null) return false
        if (suffix.contains("recycler") || suffix.contains("shelf")) return false
        return suffix in YOUTUBE_VIEWER
    }

    /** The Shorts shelf. Not `shorts_container` and not `watch_while_layout`. */
    private fun youtubeShelf(suffix: String?): Boolean {
        if (suffix == null) return false
        return suffix.contains("recycler") || suffix.contains("shelf")
    }

    /**
     * A spotlight player. The tab, its label, a recycler, and any other spotlight id are
     * the chrome Snapchat leaves up outside the player. The tab clears the latch only
     * when no player id remains in the window.
     */
    private fun snapViewer(suffix: String?): Boolean {
        if (suffix == null || !suffix.contains("spotlight")) return false
        if (CHROME_WORDS.any { suffix.contains(it) }) return false
        if (suffix.contains("recycler") || suffix.contains("shelf")) return false
        return SNAP_PLAYER.any { suffix.contains(it) }
    }

    /**
     * Inbox, Profile, or Search actually open. A tab id that merely contains "inbox"
     * is still the feed. The three composer and profile lines are not this.
     */
    private fun tiktokStructuralNonFeed(suffix: String?, node: WalkNode): Boolean {
        if (suffix != null &&
            TIKTOK_CHROME.none { suffix.contains(it) } &&
            TIKTOK_NON_FEED_IDS.any { suffix.contains(it) }
        ) {
            return true
        }
        return node.selected && exactLabel(node) in TIKTOK_NON_FEED_TABS
    }

    /** One frame of profile or composer copy on the feed. Not a selected tab. */
    private fun tiktokPhrase(node: WalkNode): Boolean {
        val label = exactLabel(node)
        return label.isNotEmpty() && label in TIKTOK_NON_FEED_PHRASES
    }

    private fun exactLabel(node: WalkNode): String {
        val text = node.text?.toString()?.trim()?.lowercase().orEmpty()
        if (text.isNotEmpty()) return text
        return node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()
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
    private const val SPOTLIGHT_TAB = "spotlight_tab"

    /** The player. Not the tab, not a post, not the 0×0 pager left on Home. */
    private val INSTAGRAM_VIEWER = setOf(
        "clips_viewer_view_pager",
        "root_clips_layout",
    )

    /**
     * The Shorts player. Each id is the full-screen player, not the shelf.
     * `reel_recycler`, `shorts_shelf`, and `shorts_container` are Home.
     */
    private val YOUTUBE_VIEWER = setOf(
        "reel_watch_fragment_root",
        "reel_player_page_container",
        "reel_player_underlay",
        "reel_player_underlay_view",
        "reel_watch_player",
        "shorts_player",
        "shorts_player_view",
        "shorts_video_pager",
    )

    /** Player-shaped spotlight ids. "spotlight" alone is the tab. */
    private val SNAP_PLAYER = listOf("player", "pager", "playback")

    /** Selected tabs that replace the player. The Reels tab is the player loading, not Home. */
    private val DESTINATION_TABS = setOf(
        "feed_tab",
        "direct_tab",
        "profile_tab",
        "search_tab",
        "camera_tab",
        "news_tab",
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

    /**
     * Inbox and DM thread ids. Not the Messages tab (`direct_tab`) and not the word
     * "messages". A thread whose bubbles or composer use these still arms.
     */
    private val PERSON_ID_PARTS = listOf(
        "inbox",
        "direct_thread",
        "thread_composer",
        "message_composer",
        "composer_content",
        "direct_text_message",
        "direct_visual_message",
        "direct_link_message",
        "direct_composer",
        "thread_message",
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

    /** Screen ids. A tab id that merely contains "inbox" is still the feed. */
    private val TIKTOK_NON_FEED_IDS = listOf(
        "inbox_list",
        "inbox_recycler",
        "chat_list",
        "message_list",
        "notification_list",
        "profile_header",
        "edit_profile",
        "search_result",
    )

    /** Selected tab labels that replace the feed. Unselected, they are chrome on For You. */
    private val TIKTOK_NON_FEED_TABS = setOf("inbox", "profile", "search")

    /** Composer and profile copy. Not "For You", and not the unselected tab label. */
    private val TIKTOK_NON_FEED_PHRASES = setOf(
        "edit profile",
        "write a message",
        "message...",
    )

    private val TIKTOK_CHROME = listOf("tab", "button", "nav", "icon", "tray", "label", "bar")

    private const val MAX_NODES = 2000
    private const val MAX_DEPTH = 30
}

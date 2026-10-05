package com.example.zen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Home, the inbox, the messages tab, and a normal post are not a viewer.
 * A block does not run again on the surface Back returns to.
 * YouTube's shelf, Snapchat's Spotlight label, and a TikTok screen that is not
 * the feed follow that same rule.
 */
class InstagramSurfaceTest {

    private val strict = ShortFormSession.Settings(
        friendPassEnabled = true,
        allowedScrolls = 0
    )

    @Test
    fun homeDoesNotBlock() {
        val reading = ScreenClass.read(IG, home())
        assertFalse(reading.shortForm)
        assertFalse(reading.personSurface)
        assertFalse(decide(reading).block)
    }

    @Test
    fun reelsLabelOnHomeChromeIsNotAViewer() {
        val reading = ScreenClass.read(IG, home())
        assertFalse(reading.shortForm)
    }

    @Test
    fun inboxDoesNotBlock() {
        val reading = ScreenClass.read(IG, inbox())
        assertFalse(reading.shortForm)
        assertTrue(reading.personSurface)
        val session = ShortFormSession()
        session.noteInAppPersonSurface(0)
        assertFalse(session.onViewer(reading.shortForm, IG, 0, strict).block)
    }

    @Test
    fun messagesTabDoesNotBlockOrArmTheNextFeed() {
        val reading = ScreenClass.read(IG, messagesTab())
        assertFalse(reading.shortForm)
        assertFalse(reading.personSurface)
        val session = ShortFormSession()
        if (reading.personSurface) session.noteInAppPersonSurface(0)
        assertFalse(session.onViewer(reading.shortForm, IG, 0, strict).block)
        assertTrue(session.onViewer(true, IG, 500, strict).block)
    }

    @Test
    fun normalPostDoesNotBlock() {
        val reading = ScreenClass.read(IG, post())
        assertFalse(reading.shortForm)
        assertFalse(reading.personSurface)
        assertFalse(decide(reading).block)
    }

    @Test
    fun openedReelsStopsOnceAndHomeDoesNot() {
        val session = ShortFormSession()
        val reels = ScreenClass.read(IG, reels())
        assertTrue(reels.shortForm)
        assertFalse(reels.personSurface)
        assertTrue(session.onViewer(reels.shortForm, IG, 0, strict).block)
        session.onBlocked()

        // Back has not landed yet. The same viewer must not block again.
        assertFalse(session.onViewer(true, IG, 400, strict).block)

        val home = ScreenClass.read(IG, home())
        assertFalse(home.shortForm)
        assertFalse(session.onViewer(home.shortForm, IG, 800, strict).block)

        // A later open, after Home, can stop once.
        assertTrue(session.onViewer(reels.shortForm, IG, 2_000, strict).block)
    }

    @Test
    fun inboxThenReelsStaysOpen() {
        val inbox = ScreenClass.read(IG, inbox())
        assertTrue(inbox.personSurface)
        val session = ShortFormSession()
        session.noteInAppPersonSurface(0)
        val reels = ScreenClass.read(IG, reels())
        val opened = session.onViewer(reels.shortForm, IG, 500, strict)
        assertTrue(opened.armed)
        assertFalse(opened.block)
    }

    @Test
    fun zeroSizePagerOnHomeIsNotAViewer() {
        val ghost = node(
            id = "com.instagram.android:id/clips_viewer_view_pager",
            onScreen = true,
            w = 0,
            h = 0,
        )
        assertFalse(ScreenClass.read(IG, ghost).shortForm)
    }

    @Test
    fun selectedHomeTabWinsOverASizedPager() {
        val window = node(
            kids = listOf(
                node(id = "com.instagram.android:id/feed_tab", desc = "Home", selected = true),
                node(id = "com.instagram.android:id/clips_viewer_view_pager", w = 1080, h = 1920),
            )
        )
        assertFalse(ScreenClass.read(IG, window).shortForm)
    }

    @Test
    fun youtubeShelfIsNotShorts() {
        val home = node(
            kids = listOf(
                node(id = "com.google.android.youtube:id/reel_recycler", desc = "Shorts", w = 1080, h = 400),
                node(desc = "Shorts"),
            )
        )
        assertFalse(ScreenClass.read(YT, home).shortForm)
        val player = node(id = "com.google.android.youtube:id/reel_watch_fragment_root", w = 1080, h = 1920)
        assertTrue(ScreenClass.read(YT, player).shortForm)
    }

    @Test
    fun spotlightLabelIsNotAViewer() {
        val tab = node(id = "com.snapchat.android:id/spotlight_tab", desc = "Spotlight")
        assertFalse(ScreenClass.read(SNAP, tab).shortForm)
        val player = node(id = "com.snapchat.android:id/spotlight_player", w = 1080, h = 1920)
        assertTrue(ScreenClass.read(SNAP, player).shortForm)
    }

    @Test
    fun youtubeShelfWordAndRecyclerAreNotThePlayer() {
        assertFalse(ScreenClass.read(YT, node(desc = "Shorts")).shortForm)
        assertFalse(ScreenClass.read(YT, node(text = "Shorts")).shortForm)
        assertFalse(
            ScreenClass.read(YT, node(id = "com.google.android.youtube:id/reel_recycler", w = 1080, h = 400)).shortForm
        )
        assertFalse(
            ScreenClass.read(YT, node(id = "com.google.android.youtube:id/shorts_recycler", desc = "Shorts")).shortForm
        )
        assertFalse(ScreenClass.read(YT, node(id = "com.google.android.youtube:id/shorts_shelf")).shortForm)
        assertFalse(ScreenClass.read(YT, node(id = "com.google.android.youtube:id/reel_shelf")).shortForm)
        assertFalse(ScreenClass.read(YT, node(id = "com.google.android.youtube:id/shorts_container")).shortForm)
        assertFalse(ScreenClass.read(YT, node(id = "com.google.android.youtube:id/watch_while_layout", w = 1080, h = 1920)).shortForm)

        val shelf = node(
            kids = listOf(
                node(id = "com.google.android.youtube:id/reel_recycler", desc = "Shorts", w = 1080, h = 400),
                node(id = "com.google.android.youtube:id/shorts_shelf", w = 1080, h = 400),
                node(desc = "Shorts"),
                node(id = "com.google.android.youtube:id/reel_watch_fragment_root", w = 0, h = 0),
            )
        )
        assertFalse(ScreenClass.read(YT, shelf).shortForm)
    }

    @Test
    fun youtubePlayerStopsOnceAndTheShelfDoesNot() {
        val shelf = node(
            kids = listOf(
                node(id = "com.google.android.youtube:id/reel_recycler", desc = "Shorts", w = 1080, h = 400),
                node(desc = "Shorts"),
            )
        )
        val player = node(
            kids = listOf(
                node(id = "com.google.android.youtube:id/reel_watch_fragment_root", w = 1080, h = 1920),
                node(id = "com.google.android.youtube:id/reel_player_page_container", w = 1080, h = 1920),
                node(id = "com.google.android.youtube:id/shorts_player", w = 1080, h = 1920),
                node(desc = "Shorts"),
                node(id = "com.google.android.youtube:id/reel_recycler", w = 1080, h = 1920),
            )
        )
        assertTrue(ScreenClass.read(YT, player).shortForm)
        assertTrue(
            ScreenClass.read(YT, node(id = "com.google.android.youtube:id/shorts_video_pager", w = 1080, h = 1920)).shortForm
        )
        stopsOnce(YT, player, shelf)
    }

    @Test
    fun spotlightChromeIsNotAPlayerAndThePlayerStopsOnce() {
        assertFalse(ScreenClass.read(SNAP, node(desc = "Spotlight")).shortForm)
        assertFalse(ScreenClass.read(SNAP, node(text = "Spotlight")).shortForm)
        assertFalse(ScreenClass.read(SNAP, node(id = "com.snapchat.android:id/spotlight")).shortForm)
        assertFalse(ScreenClass.read(SNAP, node(id = "com.snapchat.android:id/bottom_spotlight", desc = "Spotlight")).shortForm)
        assertFalse(ScreenClass.read(SNAP, node(id = "com.snapchat.android:id/spotlight_container", w = 1080, h = 400)).shortForm)
        assertFalse(ScreenClass.read(SNAP, node(id = "com.snapchat.android:id/spotlight_recycler", w = 1080, h = 800)).shortForm)
        assertFalse(
            ScreenClass.read(
                SNAP,
                node(id = "com.snapchat.android:id/spotlight_player", w = 0, h = 0)
            ).shortForm
        )

        val tab = node(id = "com.snapchat.android:id/spotlight_tab", desc = "Spotlight")
        val player = node(id = "com.snapchat.android:id/spotlight_view_pager", w = 1080, h = 1920)
        assertTrue(ScreenClass.read(SNAP, player).shortForm)
        assertTrue(
            ScreenClass.read(SNAP, node(id = "com.snapchat.android:id/spotlight_playback", w = 1080, h = 1920)).shortForm
        )
        stopsOnce(SNAP, player, tab)
    }

    @Test
    fun tiktokFeedStopsOnceAndANonFeedSurfaceDoesNot() {
        val feed = node(
            kids = listOf(
                node(desc = "For You", selected = true),
                node(desc = "Following"),
                node(id = "com.zhiliaoapp.musically:id/inbox_tab", desc = "Inbox"),
                node(id = "com.zhiliaoapp.musically:id/profile_tab", desc = "Profile"),
                node(desc = "Home", selected = true),
            )
        )
        val reading = ScreenClass.read(TT, feed)
        assertTrue(reading.shortForm)
        assertFalse(reading.personSurface)
        assertTrue(ScreenClass.read(TT, null).shortForm)

        val session = ShortFormSession()
        assertTrue(session.onViewer(reading.shortForm, TT, 0, strict).block)
        session.onBlocked()
        assertFalse(session.onViewer(true, TT, 400, strict).block)
        assertFalse(session.onScroll(true, TT, 500, strict).block)
        // A missed frame is still the feed. It must not clear the latch.
        assertFalse(session.onViewer(ScreenClass.read(TT, null).shortForm, TT, 600, strict).block)

        val inbox = node(
            kids = listOf(
                node(id = "com.zhiliaoapp.musically:id/inbox_tab", desc = "Inbox", selected = true),
                node(id = "com.zhiliaoapp.musically:id/inbox_recycler", w = 1080, h = 1600),
            )
        )
        val inboxReading = ScreenClass.read(TT, inbox)
        assertFalse(inboxReading.shortForm)
        assertFalse(inboxReading.personSurface)
        assertFalse(session.onViewer(inboxReading.shortForm, TT, 800, strict).block)

        assertTrue(session.onViewer(true, TT, 2_000, strict).block)
    }

    @Test
    fun tiktokTabLabelAndGhostListDoNotCancelTheFeed() {
        val feed = node(
            kids = listOf(
                node(desc = "For You", selected = true),
                node(desc = "Inbox"),
                node(desc = "Profile"),
                node(id = "com.zhiliaoapp.musically:id/inbox_tab", desc = "Inbox"),
                node(id = "com.zhiliaoapp.musically:id/inbox_recycler", onScreen = true, w = 0, h = 0),
            )
        )
        assertTrue(ScreenClass.read(TT, feed).shortForm)
        assertTrue(ScreenClass.read(TRILL, node(desc = "For You", selected = true)).shortForm)

        assertFalse(
            ScreenClass.read(TT, node(desc = "Inbox", selected = true)).shortForm
        )
        assertFalse(
            ScreenClass.read(TRILL, node(id = "com.ss.android.ugc.trill:id/profile_header", w = 1080, h = 400)).shortForm
        )
        assertFalse(ScreenClass.read(TT, node(text = "Edit profile")).shortForm)
        assertFalse(
            ScreenClass.read(TT, node(id = "com.zhiliaoapp.musically:id/search_result", w = 1080, h = 1600)).shortForm
        )
    }

    @Test
    fun messagesWordOnTheHomeTabBarDoesNotArm() {
        val bar = node(
            kids = listOf(
                node(id = "com.instagram.android:id/feed_tab", desc = "Home", selected = true),
                node(id = "com.instagram.android:id/clips_tab", desc = "Reels"),
                node(id = "com.instagram.android:id/direct_tab", desc = "Messages"),
                node(id = "com.instagram.android:id/profile_tab", desc = "Profile"),
                node(text = "messages"),
                node(desc = "messages"),
            )
        )
        val reading = ScreenClass.read(IG, bar)
        assertFalse(reading.shortForm)
        assertFalse(reading.personSurface)
        val session = ShortFormSession()
        if (reading.personSurface) session.noteInAppPersonSurface(0)
        assertFalse(session.onViewer(reading.shortForm, IG, 0, strict).block)
        assertTrue(session.onViewer(true, IG, 500, strict).block)
    }

    @Test
    fun directThreadArmsWithoutTheInboxList() {
        val thread = node(
            kids = listOf(
                node(id = "com.instagram.android:id/direct_text_message_text_view", text = "hey", w = 800, h = 120),
                node(id = "com.instagram.android:id/direct_composer_edit_text", w = 900, h = 100),
            )
        )
        val reading = ScreenClass.read(IG, thread)
        assertFalse(reading.shortForm)
        assertTrue(reading.personSurface)
        val session = ShortFormSession()
        if (reading.personSurface) session.noteInAppPersonSurface(0)
        val opened = session.onViewer(true, IG, 500, strict)
        assertTrue(opened.armed)
        assertFalse(opened.block)
    }

    private fun stopsOnce(packageName: String, viewer: WalkNode, returned: WalkNode) {
        val session = ShortFormSession()
        assertFalse(ScreenClass.read(packageName, returned).shortForm)
        assertTrue(ScreenClass.read(packageName, viewer).shortForm)
        assertTrue(session.onViewer(true, packageName, 0, strict).block)
        session.onBlocked()
        assertFalse(session.onViewer(true, packageName, 400, strict).block)
        assertFalse(session.onScroll(true, packageName, 500, strict).block)
        assertFalse(
            session.onViewer(ScreenClass.read(packageName, returned).shortForm, packageName, 800, strict).block
        )
        assertTrue(session.onViewer(true, packageName, 2_000, strict).block)
    }

    private fun decide(reading: ScreenClass.Reading): ShortFormSession.Decision {
        val session = ShortFormSession()
        if (reading.personSurface) session.noteInAppPersonSurface(0)
        return session.onViewer(reading.shortForm, IG, 0, strict)
    }

    private fun home(): WalkNode = node(
        kids = listOf(
            node(id = "com.instagram.android:id/feed_tab", desc = "Home", selected = true),
            node(id = "com.instagram.android:id/clips_tab", desc = "Reels"),
            node(id = "com.instagram.android:id/direct_tab", desc = "Messages"),
            node(id = "com.instagram.android:id/row_feed_photo_imageview", w = 1080, h = 1080),
            node(id = "com.instagram.android:id/clips_video_container", w = 1080, h = 600),
            node(
                id = "com.instagram.android:id/clips_viewer_view_pager",
                onScreen = true,
                w = 0,
                h = 0,
            ),
            node(desc = "reels tray container"),
        )
    )

    private fun messagesTab(): WalkNode = node(
        kids = listOf(
            node(id = "com.instagram.android:id/feed_tab", desc = "Home"),
            node(id = "com.instagram.android:id/clips_tab", desc = "Reels"),
            node(id = "com.instagram.android:id/direct_tab", desc = "Messages", selected = true),
        )
    )

    private fun inbox(): WalkNode = node(
        kids = listOf(
            node(id = "com.instagram.android:id/direct_tab", desc = "Messages", selected = true),
            node(id = "com.instagram.android:id/inbox_refreshable_thread_list_recyclerview", w = 1080, h = 1600),
            node(text = "Write a message"),
        )
    )

    private fun post(): WalkNode = node(
        kids = listOf(
            node(id = "com.instagram.android:id/feed_tab", desc = "Home", selected = true),
            node(id = "com.instagram.android:id/row_feed_photo_imageview", desc = "Photo by Sam", w = 1080, h = 1080),
            node(id = "com.instagram.android:id/clips_video_container", w = 1080, h = 800),
            node(text = "reels"),
        )
    )

    private fun reels(): WalkNode = node(
        kids = listOf(
            node(id = "com.instagram.android:id/clips_tab", desc = "Reels", selected = true),
            node(id = "com.instagram.android:id/direct_tab", desc = "Messages"),
            node(id = "com.instagram.android:id/clips_viewer_view_pager", w = 1080, h = 1920),
        )
    )

    private fun node(
        id: String? = null,
        text: String? = null,
        desc: String? = null,
        onScreen: Boolean = true,
        w: Int = 200,
        h: Int = 80,
        selected: Boolean = false,
        kids: List<WalkNode> = emptyList(),
    ): WalkNode = object : WalkNode {
        override val childCount: Int = kids.size
        override fun obtainChild(index: Int): WalkNode = kids[index]
        override fun recycle() = Unit
        override val viewIdResourceName: String? = id
        override val text: CharSequence? = text
        override val contentDescription: CharSequence? = desc
        override val packageName: CharSequence = IG
        override val visibleToUser: Boolean = onScreen
        override val width: Int = w
        override val height: Int = h
        override val selected: Boolean = selected
    }

    private companion object {
        const val IG = "com.instagram.android"
        const val YT = "com.google.android.youtube"
        const val SNAP = "com.snapchat.android"
        const val TT = "com.zhiliaoapp.musically"
        const val TRILL = "com.ss.android.ugc.trill"
    }
}

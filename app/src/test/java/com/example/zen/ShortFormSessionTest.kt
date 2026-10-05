package com.example.zen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session rule for Friend Pass. Time values are injected; nothing here waits on a clock.
 */
class ShortFormSessionTest {

    private val session = ShortFormSession()
    private val strict = ShortFormSession.Settings(
        friendPassEnabled = true,
        allowedScrolls = 0
    )

    @Test
    fun armOnce_consumedPassIsNotRereadAfterTheSessionEnds() {
        session.noteInAppPersonSurface(0)
        val first = session.onViewer(true, IG, 1_000, strict)
        assertFalse(first.block)
        assertTrue(first.armed)
        assertTrue(session.isFriendSession)

        session.onLeftGuardedApp()
        assertFalse(session.isFriendSession)

        // Still inside the original 4s window. That stamp was consumed and must not arm again.
        val second = session.onViewer(true, IG, 2_000, strict)
        assertFalse(second.armed)
        assertTrue(second.block)
    }

    @Test
    fun initialArmWindow_staysFourSeconds() {
        assertEquals(4_000L, ShortFormSession.FRIEND_PASS_WINDOW_MS)
        session.noteExternalMessenger(0)
        val late = session.onViewer(
            true,
            IG,
            ShortFormSession.FRIEND_PASS_WINDOW_MS,
            strict
        )
        assertFalse(late.armed)
        assertTrue(late.block)
    }

    @Test
    fun armedScroll_doesNotBlockOrCountTowardAllowance() {
        armAt(1_000)
        repeat(5) {
            val scrolled = session.onScroll(true, IG, 50_000, strict)
            assertFalse(scrolled.block)
            assertTrue(scrolled.armed)
        }
        assertEquals(0, session.countedScrolls)

        session.onLeftGuardedApp()
        val allowance = strict.copy(allowedScrolls = 1)
        val open = session.onViewer(true, IG, 60_000, allowance)
        assertFalse(open.armed)
        assertFalse(open.block)
        val first = session.onScroll(true, IG, 60_000, allowance)
        assertFalse(first.block)
        assertEquals(1, session.countedScrolls)
        val second = session.onScroll(true, IG, 60_000, allowance)
        assertTrue(second.block)
        assertEquals(2, session.countedScrolls)
    }

    @Test
    fun transientMiss_doesNotBlockAndDoesNotRereadTheClock() {
        armAt(1_000)
        val hidden = session.onViewer(false, IG, 100_000, strict)
        assertFalse(hidden.block)
        assertTrue(hidden.armed)
        val scrolled = session.onScroll(false, IG, 100_000, strict)
        assertFalse(scrolled.block)
        assertTrue(scrolled.armed)

        val back = session.onViewer(true, IG, 100_000, strict)
        assertFalse(back.block)
        assertTrue(back.armed)
        assertTrue(session.isFriendSession)
        assertEquals(0, session.countedScrolls)
    }

    @Test
    fun blockThenReenter_restoresPassWithoutRereadingTheClock() {
        armAt(1_000)
        val feed = session.onViewer(true, YT, 100_000, strict)
        assertTrue(feed.block)
        assertFalse(feed.armed)

        session.onBlocked()
        val flicker = session.onViewer(false, IG, 200_000, strict)
        assertFalse(flicker.block)

        val back = session.onViewer(true, IG, 200_000, strict)
        assertFalse(back.block)
        assertTrue(back.armed)

        val scrolled = session.onScroll(true, IG, 200_000, strict)
        assertFalse(scrolled.block)
        assertTrue(scrolled.armed)
        assertEquals(0, session.countedScrolls)
    }

    @Test
    fun visibleBlockedViewer_beforeFriendPackage_restoresFriendAndDoesNotArmBlocked() {
        armAt(1_000)
        val feed = session.onViewer(true, YT, 100_000, strict)
        assertTrue(feed.block)
        assertFalse(feed.armed)

        session.onBlocked()

        val stillBlocked = session.onViewer(true, YT, 200_000, strict)
        assertFalse(stillBlocked.armed)
        assertFalse(session.isFriendSession)
        if (stillBlocked.block) session.onBlocked()

        val friend = session.onViewer(true, IG, 200_000, strict)
        assertTrue(friend.armed)
        assertFalse(friend.block)
        assertTrue(session.isFriendSession)
    }

    @Test
    fun blockOnArmedSession_thenReenter_restoresPass() {
        armAt(1_000)
        session.onBlocked()
        assertFalse(session.isFriendSession)
        val back = session.onViewer(true, IG, 100_000, strict)
        assertFalse(back.block)
        assertTrue(back.armed)
    }

    @Test
    fun returnToFriendPackage_isNotChargedAsAnUnarmedFeed() {
        armAt(1_000)
        val allowance = strict.copy(allowedScrolls = 2)
        val other = session.onViewer(true, YT, 100_000, allowance)
        assertFalse(other.armed)
        assertFalse(other.block)
        val back = session.onViewer(true, IG, 100_000, allowance)
        assertTrue(back.armed)
        assertFalse(back.block)
        val scrolled = session.onScroll(true, IG, 100_000, allowance)
        assertFalse(scrolled.block)
        assertTrue(scrolled.armed)
        assertEquals(0, session.countedScrolls)
    }

    @Test
    fun unarmedFeed_contentEventsDoNotResetCountedScrolls() {
        val allowance = strict.copy(allowedScrolls = 1)
        assertFalse(session.onViewer(true, IG, 0, allowance).block)
        assertFalse(session.onScroll(true, IG, 0, allowance).block)
        assertEquals(1, session.countedScrolls)
        assertFalse(session.onViewer(true, IG, 10, allowance).block)
        assertEquals(1, session.countedScrolls)
        assertTrue(session.onScroll(true, IG, 10, allowance).block)
    }

    @Test
    fun friendPassDisabled_blocksOnLandingEvenAfterAPersonSurface() {
        session.noteInAppPersonSurface(0)
        val off = strict.copy(friendPassEnabled = false)
        val landed = session.onViewer(true, IG, 500, off)
        assertTrue(landed.block)
        assertFalse(landed.armed)
    }

    @Test
    fun neverArmed_blocksOnLandingWhenAllowanceIsZero() {
        val landed = session.onViewer(true, IG, 0, strict)
        assertTrue(landed.block)
        assertFalse(landed.armed)

        val scrolled = session.onScroll(true, YT, 0, strict)
        assertTrue(scrolled.block)
        assertFalse(scrolled.armed)
        assertEquals(0, session.countedScrolls)
    }

    @Test
    fun blockDoesNotRepeatOnTheSurfaceBackReturnsTo() {
        val landed = session.onViewer(true, IG, 0, strict)
        assertTrue(landed.block)
        session.onBlocked()

        val stillThere = session.onViewer(true, IG, 1_000, strict)
        assertFalse(stillThere.block)
        val scrolled = session.onScroll(true, IG, 1_200, strict)
        assertFalse(scrolled.block)

        // Home is not the viewer. That clears the latch, so a later open can stop once.
        assertFalse(session.onViewer(false, IG, 2_000, strict).block)
        val again = session.onViewer(true, IG, 3_000, strict)
        assertTrue(again.block)
    }

    @Test
    fun externalMessenger_doesNotByItselfRearmEndedSession() {
        session.noteExternalMessenger(0)
        val armed = session.onViewer(true, IG, 500, strict)
        assertTrue(armed.armed)

        session.onLeftGuardedApp()
        assertFalse(session.isFriendSession)

        session.noteExternalMessenger(1_000)
        assertFalse(session.isFriendSession)

        // The event did not resurrect the old session. The following viewer entry is a new arm.
        val next = session.onViewer(true, YT, 1_500, strict)
        assertTrue(next.armed)
        assertFalse(next.block)
    }

    private fun armAt(now: Long) {
        session.noteInAppPersonSurface(now - 500)
        val decision = session.onViewer(true, IG, now, strict)
        assertTrue(decision.armed)
        assertFalse(decision.block)
    }

    private companion object {
        const val IG = "com.instagram.android"
        const val YT = "com.google.android.youtube"
    }
}

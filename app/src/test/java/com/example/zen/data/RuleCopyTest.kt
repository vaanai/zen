package com.example.zen.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleCopyTest {

    @Test
    fun friendPassOn_allowanceZero_admitsTheSuggestionHole() {
        val sentence = RuleCopy.sentence(friendPassEnabled = true, allowedScrolls = 0)
        assertEquals(
            "Friends' reels stay open. A feed you open yourself stops. " +
                "A suggestion in the same viewer can't be told from a friend, so it can still play.",
            sentence
        )
        assertFalse(sentence.contains("scroll"))
    }

    @Test
    fun friendPassOn_allowanceCountsUnarmedScrolls_andKeepsTheHole() {
        assertEquals(
            "Friends' reels stay open. A feed you open yourself gets 1 scroll, then it stops. " +
                "A suggestion in the same viewer can't be told from a friend, so it can still play.",
            RuleCopy.sentence(friendPassEnabled = true, allowedScrolls = 1)
        )
        assertEquals(
            "Friends' reels stay open. A feed you open yourself gets 3 scrolls, then it stops. " +
                "A suggestion in the same viewer can't be told from a friend, so it can still play.",
            RuleCopy.sentence(friendPassEnabled = true, allowedScrolls = 3)
        )
    }

    @Test
    fun friendPassOff_stopsShortFormIncludingFriends() {
        assertEquals(
            "Short-form stops, including videos from friends.",
            RuleCopy.sentence(friendPassEnabled = false, allowedScrolls = 0)
        )
        assertEquals(
            "Short-form stops after 2 scrolls, including videos from friends.",
            RuleCopy.sentence(friendPassEnabled = false, allowedScrolls = 2)
        )
    }

    @Test
    fun tiktokNeverUsesTheFriendsOpenSentence() {
        assertEquals(
            "Opening TikTok stops. Zen can't tell a friend's video from For You here.",
            RuleCopy.TIKTOK
        )
        assertEquals(RuleCopy.TIKTOK, RuleCopy.appLimit("TikTok", friendPassEnabled = true))
        assertEquals(RuleCopy.TIKTOK, RuleCopy.appLimit("TikTok", friendPassEnabled = false))
        assertFalse(RuleCopy.TIKTOK.contains("Friends' reels"))
    }

    @Test
    fun youtubeLimitOnlyWhileFriendPassIsOn() {
        assertEquals(
            "YouTube Shorts stay open only when a message in another app just opened them. " +
                "Zen can't see a YouTube message.",
            RuleCopy.YOUTUBE
        )
        assertEquals(RuleCopy.YOUTUBE, RuleCopy.appLimit("YouTube", friendPassEnabled = true))
        assertNull(RuleCopy.appLimit("YouTube", friendPassEnabled = false))
        assertNull(RuleCopy.appLimit("Instagram", friendPassEnabled = true))
        assertNull(RuleCopy.appLimit("Snapchat", friendPassEnabled = true))
    }

    @Test
    fun limitsFollowTheAppsThatAreActuallyGuarded() {
        assertEquals(
            listOf(RuleCopy.TIKTOK, RuleCopy.YOUTUBE),
            RuleCopy.limits(friendPassEnabled = true, tiktokGuarded = true, youtubeGuarded = true)
        )
        assertEquals(
            listOf(RuleCopy.TIKTOK),
            RuleCopy.limits(friendPassEnabled = false, tiktokGuarded = true, youtubeGuarded = true)
        )
        assertTrue(
            RuleCopy.limits(friendPassEnabled = true, tiktokGuarded = false, youtubeGuarded = false).isEmpty()
        )
    }

    @Test
    fun neverSaysOneVideoThenTheNextScroll() {
        val lines = buildList {
            add(RuleCopy.sentence(true, 0))
            add(RuleCopy.sentence(true, 1))
            add(RuleCopy.sentence(true, 5))
            add(RuleCopy.sentence(false, 0))
            add(RuleCopy.sentence(false, 4))
            add(RuleCopy.TIKTOK)
            add(RuleCopy.YOUTUBE)
        }
        val banned = listOf("one video", "next scroll", "dm'd", "block the next")
        for (line in lines) {
            val lower = line.lowercase()
            for (phrase in banned) {
                assertFalse("$phrase in: $line", lower.contains(phrase))
            }
        }
    }
}

package com.example.zen.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardModeTest {

    @Test
    fun threeModesWriteThePrefsTheServiceReads() {
        assertEquals(KeptRule(true, 0), GuardMode.FRIENDS_OPEN.toRule())
        assertEquals(KeptRule(true, 1), GuardMode.A_FEW_SCROLLS.toRule(1))
        assertEquals(KeptRule(true, 2), GuardMode.A_FEW_SCROLLS.toRule(2))
        assertEquals(KeptRule(true, 3), GuardMode.A_FEW_SCROLLS.toRule(3))
        assertEquals(KeptRule(false, 0), GuardMode.ALL_STOPS.toRule())
    }

    @Test
    fun aFewScrollsStaysInsideOneToThree() {
        assertEquals(KeptRule(true, 1), GuardMode.A_FEW_SCROLLS.toRule(0))
        assertEquals(KeptRule(true, 3), GuardMode.A_FEW_SCROLLS.toRule(9))
    }

    @Test
    fun fromPrefsMatchesOnlyTheThreeModes() {
        assertEquals(GuardMode.FRIENDS_OPEN, GuardMode.fromPrefs(true, 0))
        assertEquals(GuardMode.A_FEW_SCROLLS, GuardMode.fromPrefs(true, 2))
        assertEquals(GuardMode.ALL_STOPS, GuardMode.fromPrefs(false, 0))
        assertNull(GuardMode.fromPrefs(false, 2))
        assertNull(GuardMode.fromPrefs(true, 5))
    }

    @Test
    fun keptCollapsesALeftoverAllowanceToFriendsOpen() {
        assertEquals(KeptRule(true, 0), GuardMode.kept(true, 0))
        assertEquals(KeptRule(true, 3), GuardMode.kept(true, 3))
        assertEquals(KeptRule(false, 0), GuardMode.kept(false, 0))
        assertEquals(KeptRule(true, 0), GuardMode.kept(false, 4))
        assertEquals(KeptRule(true, 0), GuardMode.kept(true, 5))
    }
}

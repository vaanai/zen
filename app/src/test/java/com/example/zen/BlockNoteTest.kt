package com.example.zen

import com.example.zen.data.RuleCopy
import com.example.zen.persona.LineLibrary
import com.example.zen.persona.Persona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockNoteTest {

    @Test
    fun noteIsTheShortNoun() {
        assertEquals("That's the feed.", BlockNote.LINE)
        assertEquals(1, BlockNote.LINE.count { it == '.' })
        assertFalse(BlockNote.LINE.contains('\n'))
    }

    @Test
    fun noteIsNotTheHomeSentence() {
        val homes = listOf(
            RuleCopy.sentence(friendPassEnabled = true, allowedScrolls = 0),
            RuleCopy.sentence(friendPassEnabled = true, allowedScrolls = 3),
            RuleCopy.sentence(friendPassEnabled = false, allowedScrolls = 0),
            RuleCopy.sentence(friendPassEnabled = false, allowedScrolls = 2),
            RuleCopy.TIKTOK,
            RuleCopy.YOUTUBE
        )
        homes.forEach { home ->
            assertFalse(home == BlockNote.LINE)
            assertTrue(home.length > BlockNote.LINE.length)
        }
        assertFalse(BlockNote.LINE.contains("Friends'"))
        assertFalse(BlockNote.LINE.contains("scroll"))
    }

    @Test
    fun noteIsNotAPersonaRoast() {
        listOf(0, 1, 2, 3, 8).forEach { relapse ->
            Persona.entries.forEach { persona ->
                assertFalse(LineLibrary.blockLine(persona, relapse) == BlockNote.LINE)
            }
        }
    }

    @Test
    fun noteIsGoneInUnderASecond() {
        assertTrue(BlockNote.DISMISS_AFTER_MS < 1_000L)
        assertTrue(BlockNote.DISMISS_AFTER_MS >= 600L)
    }

    @Test
    fun noteDoesNotTakeTheNextTap() {
        assertTrue(BlockNote.PASSES_TOUCHES)
    }
}

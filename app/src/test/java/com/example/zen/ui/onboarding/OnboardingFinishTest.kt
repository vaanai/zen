package com.example.zen.ui.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFinishTest {

    @Test
    fun blankPasswordFinishesOnlyWhenAccessibilityIsOn() {
        assertTrue(onboardingMayFinish(accessibilityEnabled = true, password = ""))
        assertFalse(onboardingMayFinish(accessibilityEnabled = false, password = ""))
    }

    @Test
    fun exactPasswordFinishesOnlyWhenAccessibilityIsOn() {
        val password = "a".repeat(15)
        assertTrue(onboardingMayFinish(accessibilityEnabled = true, password = password))
        assertFalse(onboardingMayFinish(accessibilityEnabled = false, password = password))
    }

    @Test
    fun aDifferentLengthIsNotFinishedEvenIfAccessibilityIsOn() {
        assertFalse(onboardingMayFinish(accessibilityEnabled = true, password = "short"))
        assertFalse(onboardingMayFinish(accessibilityEnabled = true, password = "a".repeat(14)))
        assertFalse(onboardingMayFinish(accessibilityEnabled = true, password = "a".repeat(16)))
        assertFalse(onboardingMayFinish(accessibilityEnabled = false, password = "short"))
    }
}

package com.example.zen.ui.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFinishTest {

    @Test
    fun finishesWhenAccessibilityIsOn() {
        assertTrue(onboardingMayFinish(accessibilityEnabled = true))
    }

    @Test
    fun doesNotFinishWhileAccessibilityIsOff() {
        assertFalse(onboardingMayFinish(accessibilityEnabled = false))
    }
}

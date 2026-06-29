package com.example.zen.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import com.example.zen.persona.PersonaTheme
import org.junit.Rule
import org.junit.Test

/** Smoke test that the persona theme renders and exposes its colors to the tree. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun personaTheme_rendersContent() {
    composeTestRule.setContent {
      PersonaTheme(Persona.GOBLIN) {
        // Reading LocalPersonaColors proves the theme provides its accents to descendants.
        val colors = LocalPersonaColors.current
        Text(if (colors.isLight) "light" else "Goblin active")
      }
    }
    composeTestRule.onNodeWithText("Goblin active").assertExists()
  }
}

package com.example.zen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.zen.data.DefaultZenStatusProvider
import com.example.zen.data.ZenPrefs
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.PersonaTheme
import com.example.zen.ui.main.MainScreen
import com.example.zen.ui.main.MainScreenViewModel
import com.example.zen.ui.onboarding.OnboardingScreen
import com.example.zen.ui.settings.SettingsScreen
import kotlinx.coroutines.delay

/**
 * Root of the app. Holds the active persona (so switching it reskins everything live), wraps the
 * persona theme, and hosts onboarding → dashboard → settings navigation.
 */
@Composable
fun ZenApp() {
    val context = LocalContext.current
    val prefs = remember { ZenPrefs(context.applicationContext) }
    val statusProvider = remember { DefaultZenStatusProvider(context.applicationContext) }

    var persona by remember { mutableStateOf(prefs.persona) }
    val setPersona: (com.example.zen.persona.Persona) -> Unit = { p -> persona = p; prefs.persona = p }

    // Live permission status for onboarding / dashboard.
    var accessibilityEnabled by remember { mutableStateOf(statusProvider.isAccessibilityEnabled()) }
    var usageEnabled by remember { mutableStateOf(statusProvider.isUsageAccessEnabled()) }
    LaunchedEffect(Unit) {
        while (true) {
            accessibilityEnabled = statusProvider.isAccessibilityEnabled()
            usageEnabled = statusProvider.isUsageAccessEnabled()
            delay(1500)
        }
    }
    // Lenient mode is not a rule these screens state. Leave it off so the service
    // does not add a scroll the sentence never mentions.
    LaunchedEffect(Unit) {
        if (prefs.earnedScrollsEnabled) prefs.earnedScrollsEnabled = false
    }

    PersonaTheme(persona) {
        PersonaSystemBars()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val backStack = rememberNavBackStack(if (prefs.onboardingComplete) Dashboard else Onboarding)

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = {
                        (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 10 }) togetherWith
                            fadeOut(tween(180))
                    },
                    popTransitionSpec = {
                        fadeIn(tween(280)) togetherWith
                            (fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 10 })
                    },
                    entryProvider = entryProvider {
                        entry<Onboarding> {
                            OnboardingScreen(
                                prefs = prefs,
                                selectedPersona = persona,
                                onPersonaSelected = setPersona,
                                isAccessibilityEnabled = accessibilityEnabled,
                                isUsageEnabled = usageEnabled,
                                onFinish = {
                                    backStack.clear()
                                    backStack.add(Dashboard)
                                }
                            )
                        }
                        entry<Dashboard> {
                            val viewModel: MainScreenViewModel =
                                viewModel { MainScreenViewModel(statusProvider, prefs) }
                            MainScreen(
                                viewModel = viewModel,
                                onOpenSettings = { backStack.add(SettingsRoute) }
                            )
                        }
                        entry<SettingsRoute> {
                            SettingsScreen(
                                prefs = prefs,
                                selectedPersona = persona,
                                onPersonaSelected = setPersona,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Status and navigation icons follow the persona.
 * Light paper (Zen, Sage) gets dark icons. Dark personas get light icons.
 * The app theme is light, so [androidx.activity.enableEdgeToEdge] with no style would
 * leave dark icons on Goblin and Coach.
 */
@Composable
private fun PersonaSystemBars() {
    val view = LocalView.current
    val light = LocalPersonaColors.current.isLight
    if (view.isInEditMode) return
    LaunchedEffect(light) {
        val activity = view.context.findActivity() as? ComponentActivity ?: return@LaunchedEffect
        activity.applyPersonaSystemBars(light)
    }
}

internal fun ComponentActivity.applyPersonaSystemBars(isLight: Boolean) {
    val style = if (isLight) {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    } else {
        SystemBarStyle.dark(Color.TRANSPARENT)
    }
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

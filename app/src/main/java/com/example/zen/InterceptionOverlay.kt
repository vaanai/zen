package com.example.zen

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.zen.persona.LocalPersonaColors
import com.example.zen.persona.Persona
import com.example.zen.persona.PersonaTheme
import com.example.zen.ui.components.GlassCard
import com.example.zen.ui.components.PersonaSigil
import com.example.zen.ui.design.ZenSpacing

/**
 * The themed block-interception screen, rendered with Compose inside a
 * [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] window owned by the Accessibility Service
 * (no `SYSTEM_ALERT_WINDOW` needed). The persona "materializes" over the screen, then it
 * auto-dismisses after a short beat (the service has already backed out underneath).
 *
 * Because the overlay lives outside an Activity, we host the [ComposeView] with our own minimal
 * lifecycle / saved-state / view-model owners so Compose can run.
 */
class InterceptionOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var current: View? = null
    private var host: OverlayLifecycleOwner? = null

    fun show(persona: Persona, line: String) {
        handler.post {
            removeNow()

            val lifecycleOwner = OverlayLifecycleOwner().apply { onCreate() }
            val composeView = ComposeView(service).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    PersonaTheme(persona) {
                        BlockContent(line = line, onDismiss = { removeNow() })
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            try {
                windowManager.addView(composeView, params)
                lifecycleOwner.onResume()
                current = composeView
                host = lifecycleOwner
                handler.postDelayed({ removeNow() }, DISMISS_AFTER_MS)
            } catch (e: Exception) {
                lifecycleOwner.onDestroy()
                current = null
                host = null
            }
        }
    }

    private fun removeNow() {
        current?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (_: Exception) {
            }
        }
        host?.onDestroy()
        current = null
        host = null
    }

    companion object {
        private const val DISMISS_AFTER_MS = 2400L
    }
}

@Composable
private fun BlockContent(line: String, onDismiss: () -> Unit) {
    val c = LocalPersonaColors.current

    // Materialize entrance: fade + gentle scale-up on first composition.
    var shown by remember { mutableStateOf(false) }
    val enter by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(420, easing = EaseOutBack),
        label = "overlayEnter"
    )
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.gradient))
            .clickable(interactionSource = interaction, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .padding(horizontal = ZenSpacing.xl)
                .scale(0.92f + 0.08f * enter)
                .alpha(enter),
            contentPadding = ZenSpacing.xxl
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PersonaSigil(size = 72.dp, glyphSize = 40.dp)
                Spacer(Modifier.height(ZenSpacing.xl))
                Text(
                    text = line,
                    style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.sp, fontSize = 24.sp),
                    color = c.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(ZenSpacing.xl))
                Text(
                    text = "tap to dismiss".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Minimal lifecycle / saved-state / view-model owner so a [ComposeView] can run outside an Activity. */
private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

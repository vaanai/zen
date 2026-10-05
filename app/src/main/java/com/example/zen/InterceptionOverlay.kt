package com.example.zen

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.zen.ui.design.ZenElevation
import com.example.zen.ui.design.ZenRadius
import com.example.zen.ui.design.ZenSpacing
import kotlinx.coroutines.delay

/**
 * A short note over the social app, in a
 * [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] owned by the accessibility service.
 *
 * The window stays clear except for one sentence. [BlockNote.DISMISS_AFTER_MS] removes it.
 * A tap anywhere on the window may dismiss early. The window is not focusable and is not
 * [WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE]; the timer does not depend on the tap.
 *
 * The overlay lives outside an Activity, so the [ComposeView] gets its own lifecycle,
 * saved-state, and view-model owners.
 */
class InterceptionOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { removeNow() }
    private var current: View? = null
    private var host: OverlayLifecycleOwner? = null

    fun show(persona: Persona) {
        handler.post {
            removeNow()

            val lifecycleOwner = OverlayLifecycleOwner().apply { onCreate() }
            val composeView = ComposeView(service).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    PersonaTheme(persona) {
                        BlockContent(onDismiss = { removeNow() })
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
                handler.postDelayed(dismissRunnable, BlockNote.DISMISS_AFTER_MS)
            } catch (e: Exception) {
                lifecycleOwner.onDestroy()
                current = null
                host = null
            }
        }
    }

    private fun removeNow() {
        // One block's dismiss must not remove the next overlay.
        handler.removeCallbacks(dismissRunnable)
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
}

/** Fade in, and start leaving just before the timer removes the window. */
private const val NOTE_FADE_MS = 140

/**
 * Dark enough to read one line over a video, translucent enough that the feed shows through.
 * The rest of the window has no fill.
 */
private const val NOTE_VEIL_ALPHA = 0.58f

@Composable
private fun BlockContent(onDismiss: () -> Unit) {
    val c = LocalPersonaColors.current
    var shown by remember { mutableStateOf(false) }
    val presence by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(NOTE_FADE_MS),
        label = "note"
    )
    LaunchedEffect(Unit) {
        shown = true
        delay(BlockNote.DISMISS_AFTER_MS - NOTE_FADE_MS)
        shown = false
    }

    val veil = if (c.isLight) c.textPrimary else c.gradient.first()
    val ink = if (c.isLight) c.gradient.first() else c.textPrimary
    val shape = ZenRadius.pill

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .navigationBarsPadding()
            .padding(horizontal = ZenSpacing.xl, vertical = ZenSpacing.xxl),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = BlockNote.LINE,
            modifier = Modifier
                .alpha(presence)
                .clip(shape)
                .background(veil.copy(alpha = NOTE_VEIL_ALPHA))
                .border(ZenElevation.hairline, c.accent.copy(alpha = 0.55f), shape)
                .padding(horizontal = ZenSpacing.lg, vertical = ZenSpacing.sm),
            style = MaterialTheme.typography.bodyLarge.copy(
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Medium
            ),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

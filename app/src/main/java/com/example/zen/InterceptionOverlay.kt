package com.example.zen

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.example.zen.persona.Persona
import com.example.zen.persona.PersonaPalette

/**
 * The themed block-interception screen. Rendered as a [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]
 * window owned by the Accessibility Service — this requires NO `SYSTEM_ALERT_WINDOW` permission.
 * Auto-dismisses after a short beat (the service has already triggered a back-out underneath).
 */
class InterceptionOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var current: View? = null

    fun show(persona: Persona, line: String) {
        handler.post {
            removeNow()
            val colors = PersonaPalette.of(persona)

            val root = FrameLayout(service).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        colors.gradient[0].toArgb(),
                        colors.gradient[1].toArgb(),
                        colors.gradient[2].toArgb()
                    )
                )
                setOnClickListener { removeNow() }
            }

            val column = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(40), 0, dp(40), 0)
            }

            val glyph = TextView(service).apply {
                text = persona.glyph
                textSize = 56f
                gravity = Gravity.CENTER
            }

            val message = TextView(service).apply {
                text = line
                setTextColor(colors.textPrimary.toArgb())
                textSize = 24f
                gravity = Gravity.CENTER
                typeface = if (persona == Persona.SAGE) Typeface.SERIF else Typeface.DEFAULT_BOLD
                setPadding(0, dp(24), 0, 0)
            }

            val hint = TextView(service).apply {
                text = "tap to dismiss"
                setTextColor(colors.textSecondary.toArgb())
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(28), 0, 0)
            }

            column.addView(glyph)
            column.addView(message)
            column.addView(hint)

            root.addView(
                column,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            try {
                windowManager.addView(root, params)
                current = root
                handler.postDelayed({ removeNow() }, DISMISS_AFTER_MS)
            } catch (e: Exception) {
                current = null
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
        current = null
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()

    companion object {
        private const val DISMISS_AFTER_MS = 2400L
    }
}

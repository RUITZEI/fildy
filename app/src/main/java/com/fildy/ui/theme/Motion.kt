package com.fildy.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

// Motion tokens (Material 3 easing curves + durations) from the design system.
object FildyMotion {
    /** Screen changes, sphere → ✓. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val EmphasizedDuration = 500

    /** Entrances: cards, dialogs. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    const val EmphasizedDecelerateDuration = 400

    /** Exits. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    const val EmphasizedAccelerateDuration = 200

    /** Small changes: color, opacity. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val StandardDuration = 300
}

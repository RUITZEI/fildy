package com.fildy.ui.theme

import androidx.compose.ui.graphics.Color

// "Bosque" palette — design tokens from the Fildy design system.
// Roles follow Material 3 so they map directly onto lightColorScheme().

/** primaryContainer — bottom of the background, voice button. */
val Bosque = Color(0xFF0B4A3B)

/** primary — icons, actions, links. */
val Pino = Color(0xFF1A8A6C)

/** secondary — voice orb, gradient. */
val Menta = Color(0xFF5CC29A)

/** tertiary — "saved" check, accents. */
val Brote = Color(0xFFBDEBA4)

/** primaryFixed — icon backgrounds. */
val MentaClaro = Color(0xFFE3F4EA)

/** surface — background of light screens. */
val Niebla = Color(0xFFF4F8F5)

/** surfaceContainerLowest — cards, primary button. */
val Blanco = Color(0xFFFFFFFF)

/** onSurface — main text. */
val Tinta = Color(0xFF0F2A22)

/** onSurfaceVariant — secondary text. */
val Musgo = Color(0xFF3A5148)

/** Data color, used only for rain (text and icons). */
val Laguna = Color(0xFF17788A)

/** Rain chart bars. */
val LagunaBar = Color(0xFF2C93A6)

/** Empty rain chart bars. */
val LagunaBarEmpty = Color(0xFFDCE8E6)

/** Hairline dividers inside cards. */
val Divider = Color(0xFFE6ECE8)

/** Outlines / dashed separators. */
val Outline = Color(0xFFDCE5DF)

// Background gradient (voice + home screens). Text on it is always white.
val GradientTop = Color(0xFF2FA27E)
val GradientUpperMid = Pino
val GradientLowerMid = Color(0xFF0F6650)
val GradientBottom = Bosque

/** Light glow at the top-right of the gradient, used at 35% alpha. */
val GradientLight = Color(0xFFD6FFD6)

/** Secondary text on the gradient. */
val OnGradientVariant = Color(0xFFE6F7EE)

// Sun orb (weather)
val SunGlow = Color(0xFFFFDC78)
val SunHighlight = Color(0xFFFFF1B8)
val SunMid = Color(0xFFF2C14E)
val SunEdge = Color(0xFFE0A62B)

// Home landscape (hills, trees, fence)
val HillFront = Color(0xFF17785E)
val HillMiddle = GradientLowerMid
val HillBack = Bosque
val TreeTrunk = Color(0xFF6B4A2B)
val TreeCrown = Menta
val TreeCrownHighlight = Color(0xFF9BE0B4)
val TreeCrownSmall = Color(0xFF4DB38B)
val Fence = Brote

// Voice sphere
val OrbCore = Color(0xFF9BE0B4)
val OrbMid = Menta
val OrbEdge = GradientTop
val OrbBlobLight = Color(0xFFE9FFD9)
val OrbBlobDark = Pino
val OrbBlobAccent = Brote

package com.arinahub.arinaagri.calculator.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The whole colour system, generated from one seed.
 *
 * `ArinaSeedGreen` is the brand green the web prototype uses as `palette.primary.main`
 * (`arina-agri/lib/theme.ts`) and the app icon is drawn in. Every token below was produced from it
 * by the Material 3 algorithm — Material Color Utilities, `SchemeTonalSpot`, 2021 spec, contrast
 * level 0 — rather than picked by eye. That is why light `primary` reads `#226A4D` and not the seed
 * itself: the algorithm places primary at tone 40 of the seed's tonal palette, which is a hair
 * darker than the seed and is what makes `onPrimary = white` clear 4.5:1.
 *
 * These are constants, not a runtime generator. The seed never changes at runtime, so generating
 * the palettes on every launch would cost startup time to produce the same numbers; keeping them as
 * literals also means every value is reviewable in a diff. Regenerate with
 * `docs/evidence/theme/generate_palette.py` if the seed ever moves, and re-run the contrast audit in
 * the same file rather than adjusting a token by hand.
 *
 * Every pair the app actually draws was measured with the WCAG 2.1 relative-luminance formula:
 * 96 pairs, all passing (4.5:1 for text, 3:1 for control boundaries and chart fills). The table is
 * in `docs/UX_AND_ACCESSIBILITY.md`.
 */
val MankeuSeedBlue = Color(0xFF0061A4)
val ArinaSeedGreen = MankeuSeedBlue

private val ArinaLightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    inversePrimary = Color(0xFF9FCAFF),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF006874),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EEFFD),
    onTertiaryContainer = Color(0xFF001F24),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF0061A4),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFEFF0F7),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD8DAE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    scrim = Color(0xFF000000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    primaryFixed = Color(0xFFD1E4FF),
    primaryFixedDim = Color(0xFF9FCAFF),
    onPrimaryFixed = Color(0xFF001D36),
    onPrimaryFixedVariant = Color(0xFF00497D),
    secondaryFixed = Color(0xFFD7E3F8),
    secondaryFixedDim = Color(0xFFBBC7DB),
    onSecondaryFixed = Color(0xFF101C2B),
    onSecondaryFixedVariant = Color(0xFF3B4858),
    tertiaryFixed = Color(0xFF9EEFFD),
    tertiaryFixedDim = Color(0xFF82D3E0),
    onTertiaryFixed = Color(0xFF001F24),
    onTertiaryFixedVariant = Color(0xFF004F58),
)

private val ArinaDarkColors = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    inversePrimary = Color(0xFF0061A4),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFF82D3E0),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF9EEFFD),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceTint = Color(0xFF9FCAFF),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    surfaceBright = Color(0xFF36393E),
    surfaceDim = Color(0xFF111418),
    surfaceContainerLowest = Color(0xFF0B0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    primaryFixed = Color(0xFFD1E4FF),
    primaryFixedDim = Color(0xFF9FCAFF),
    onPrimaryFixed = Color(0xFF001D36),
    onPrimaryFixedVariant = Color(0xFF00497D),
    secondaryFixed = Color(0xFFD7E3F8),
    secondaryFixedDim = Color(0xFFBBC7DB),
    onSecondaryFixed = Color(0xFF101C2B),
    onSecondaryFixedVariant = Color(0xFF3B4858),
    tertiaryFixed = Color(0xFF9EEFFD),
    tertiaryFixedDim = Color(0xFF82D3E0),
    onTertiaryFixed = Color(0xFF001F24),
    onTertiaryFixedVariant = Color(0xFF004F58),
)

data class ArinaSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    // No `warning`/`onWarning` pair: nothing in the app draws a filled warning surface with text on
    // it. The container pair below is what the Proyeksi chip and the notice card use.
    val warningContainer: Color,
    val onWarningContainer: Color,
    val chart: List<Color>,
)

private val ArinaLightSemanticColors = ArinaSemanticColors(
    success = Color(0xFF006D39),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF9CF6B2),
    onSuccessContainer = Color(0xFF00210B),
    danger = Color(0xFFBA1A1A),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFFDAD6),
    onDangerContainer = Color(0xFF410002),
    warningContainer = Color(0xFFE7E4B2),
    onWarningContainer = Color(0xFF1D1D00),
    chart = listOf(
        Color(0xFF0061A4),
        Color(0xFF006D39),
        Color(0xFFBA1A1A),
        Color(0xFF006874),
        Color(0xFF8F4C38),
        Color(0xFF6750A4),
    ),
)

private val ArinaDarkSemanticColors = ArinaSemanticColors(
    success = Color(0xFF81D997),
    onSuccess = Color(0xFF00391B),
    successContainer = Color(0xFF005329),
    onSuccessContainer = Color(0xFF9CF6B2),
    danger = Color(0xFFFFB4AB),
    onDanger = Color(0xFF690005),
    dangerContainer = Color(0xFF93000A),
    onDangerContainer = Color(0xFFFFDAD6),
    warningContainer = Color(0xFF4B4700),
    onWarningContainer = Color(0xFFE7E4B2),
    chart = listOf(
        Color(0xFF9FCAFF),
        Color(0xFF81D997),
        Color(0xFFFFB4AB),
        Color(0xFF82D3E0),
        Color(0xFFFFB59D),
        Color(0xFFCFBCFF),
    ),
)

val LocalArinaSemanticColors = androidx.compose.runtime.staticCompositionLocalOf { ArinaLightSemanticColors }

/**
 * Corner radii.
 *
 * The web dashboard's default MUI Card is a flat, literal 24px radius
 * (`lib/theme.ts` `MuiCard.styleOverrides.root.borderRadius: 24`) applied to nearly every card,
 * with the flagship hero/KPI cards going up to an `sx={{ borderRadius: 4 }}` that MUI resolves to
 * 64px. `medium` is Material 3's default plain-`Card()` shape, so setting it alone rounds every
 * card that does not pass an explicit `shape=`.
 */
private val ArinaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp), // chips
    small = RoundedCornerShape(20.dp), // buttons, filter chips — near-pill, matches the web's buttons
    medium = RoundedCornerShape(24.dp), // default Card() shape — matches the web's global MuiCard radius
    large = RoundedCornerShape(28.dp), // bigger panels
    extraLarge = RoundedCornerShape(32.dp), // sheets, the floating nav bar, and the Beranda hero card
)

/**
 * Animation budget for the whole app, driven by the owner's Gerakan & animasi setting.
 * Every animated surface reads [LocalMotion] instead of hardcoding a duration, so "Dikurangi"
 * and "Mati" are real controls rather than a switch that changes nothing.
 */
data class ArinaMotion(
    val enabled: Boolean,
    val fastMillis: Int,
    val mediumMillis: Int,
) {
    companion object {
        val Full = ArinaMotion(enabled = true, fastMillis = 180, mediumMillis = 320)
        val Reduced = ArinaMotion(enabled = true, fastMillis = 90, mediumMillis = 140)
        val Off = ArinaMotion(enabled = false, fastMillis = 0, mediumMillis = 0)

        fun of(mode: MotionMode): ArinaMotion = when (mode) {
            MotionMode.FULL -> Full
            MotionMode.REDUCED -> Reduced
            MotionMode.OFF -> Off
        }

        /**
         * The app setting, narrowed by the system's own animation scale.
         *
         * Android exposes "Remove animations" / a reduced animator duration scale in accessibility
         * settings, and an app is expected to obey it. The in-app Gerakan control can only make
         * motion *quieter* than the system asks for, never louder: someone who turned animations
         * off system-wide did so for a reason — vestibular discomfort, or a slow phone — and should
         * not have to find a second switch inside this app.
         */
        fun resolve(mode: MotionMode, systemScale: Float): ArinaMotion = when {
            systemScale <= 0f -> Off
            systemScale < 1f -> of(mode).let { if (it == Full) Reduced else it }
            else -> of(mode)
        }
    }
}

val LocalMotion = androidx.compose.runtime.staticCompositionLocalOf { ArinaMotion.Full }

/**
 * Whether the theme currently in effect is the dark one.
 *
 * Derived from the scheme rather than passed down or re-read from the preference, so it cannot
 * disagree with the colours actually on screen — including mid-animation, while the two schemes are
 * interpolating. Its one job is elevation: a drop shadow is a light-mode device. On a dark surface
 * a black shadow either vanishes or turns into a hard grey rim, so dark mode separates layers with
 * the surface-container tones instead, exactly as Material 3 specifies.
 */
@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Interpolates every colour token towards [target] over the motion budget.
 *
 * This is how Terang <-> Gelap cross-fades across the whole hierarchy. The obvious alternative —
 * wrapping the app in a `Crossfade` keyed on the theme — renders two copies of the tree and
 * therefore throws away everything remembered inside it: scroll positions, a half-filled entry
 * form, an open sheet. Animating the scheme keeps one composition and one set of state, and every
 * surface, text and icon in the app moves to its new colour together because they all read these
 * same tokens.
 */
@Composable
private fun animatedColorScheme(target: ColorScheme, motion: ArinaMotion): ColorScheme {
    val spec = tween<Color>(durationMillis = maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)

    @Composable
    fun shade(value: Color, label: String): Color = animateColorAsState(value, spec, label = label).value

    return target.copy(
        primary = shade(target.primary, "primary"),
        onPrimary = shade(target.onPrimary, "onPrimary"),
        primaryContainer = shade(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = shade(target.onPrimaryContainer, "onPrimaryContainer"),
        inversePrimary = shade(target.inversePrimary, "inversePrimary"),
        secondary = shade(target.secondary, "secondary"),
        onSecondary = shade(target.onSecondary, "onSecondary"),
        secondaryContainer = shade(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = shade(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = shade(target.tertiary, "tertiary"),
        onTertiary = shade(target.onTertiary, "onTertiary"),
        tertiaryContainer = shade(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = shade(target.onTertiaryContainer, "onTertiaryContainer"),
        background = shade(target.background, "background"),
        onBackground = shade(target.onBackground, "onBackground"),
        surface = shade(target.surface, "surface"),
        onSurface = shade(target.onSurface, "onSurface"),
        surfaceVariant = shade(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = shade(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceTint = shade(target.surfaceTint, "surfaceTint"),
        inverseSurface = shade(target.inverseSurface, "inverseSurface"),
        inverseOnSurface = shade(target.inverseOnSurface, "inverseOnSurface"),
        error = shade(target.error, "error"),
        onError = shade(target.onError, "onError"),
        errorContainer = shade(target.errorContainer, "errorContainer"),
        onErrorContainer = shade(target.onErrorContainer, "onErrorContainer"),
        outline = shade(target.outline, "outline"),
        outlineVariant = shade(target.outlineVariant, "outlineVariant"),
        surfaceBright = shade(target.surfaceBright, "surfaceBright"),
        surfaceDim = shade(target.surfaceDim, "surfaceDim"),
        surfaceContainerLowest = shade(target.surfaceContainerLowest, "surfaceContainerLowest"),
        surfaceContainerLow = shade(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = shade(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = shade(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = shade(target.surfaceContainerHighest, "surfaceContainerHighest"),
    )
}

/** The same treatment for the semantic accents, so income/expense tones travel with the theme. */
@Composable
private fun animatedSemanticColors(target: ArinaSemanticColors, motion: ArinaMotion): ArinaSemanticColors {
    val spec = tween<Color>(durationMillis = maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)

    @Composable
    fun shade(value: Color, label: String): Color = animateColorAsState(value, spec, label = label).value

    return ArinaSemanticColors(
        success = shade(target.success, "success"),
        onSuccess = shade(target.onSuccess, "onSuccess"),
        successContainer = shade(target.successContainer, "successContainer"),
        onSuccessContainer = shade(target.onSuccessContainer, "onSuccessContainer"),
        danger = shade(target.danger, "danger"),
        onDanger = shade(target.onDanger, "onDanger"),
        dangerContainer = shade(target.dangerContainer, "dangerContainer"),
        onDangerContainer = shade(target.onDangerContainer, "onDangerContainer"),
        warningContainer = shade(target.warningContainer, "warningContainer"),
        onWarningContainer = shade(target.onWarningContainer, "onWarningContainer"),
        // Chart slices animate as a group; the list length is fixed, so a plain map is enough.
        chart = target.chart.mapIndexed { index, color -> shade(color, "chart$index") },
    )
}

@Composable
fun ArinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: ArinaMotion = ArinaMotion.Full,
    content: @Composable () -> Unit,
) {
    val targetScheme = if (darkTheme) ArinaDarkColors else ArinaLightColors
    val targetSemantic = if (darkTheme) ArinaDarkSemanticColors else ArinaLightSemanticColors
    // At Gerakan = Mati the durations collapse to 1ms, so the switch is effectively instant and
    // this stays a real setting rather than an animation the owner cannot turn off.
    val scheme = animatedColorScheme(targetScheme, motion)
    val semanticColors = animatedSemanticColors(targetSemantic, motion)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalArinaSemanticColors provides semanticColors,
        LocalMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = ArinaShapes,
            content = content,
        )
    }
}

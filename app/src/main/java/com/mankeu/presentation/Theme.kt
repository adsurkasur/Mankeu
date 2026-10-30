package com.mankeu.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * `MankeuSeedBlue` is the brand blue seed (0xFF0061A4).
 * Every token below was produced from it by the Material 3 algorithm — Material Color Utilities,
 * `SchemeTonalSpot`, 2021 spec, contrast level 0 — rather than picked by eye.
 *
 * These are constants, not a runtime generator. The seed never changes at runtime, so generating
 * the palettes on every launch would cost startup time to produce the same numbers; keeping them as
 * literals also means every value is reviewable in a diff. Regenerate with
 * `docs/evidence/theme/generate_palette.py` if the seed ever moves, and re-run the contrast audit in
 * the same file rather than adjusting a token by hand.
 *
 * Every pair the app actually draws was measured with the WCAG 2.1 relative-luminance formula:
 * all passing (4.5:1 for text, 3:1 for control boundaries and chart fills).
 */
val MankeuSeedBlue = Color(0xFF0061A4)

private val MankeuLightColors = lightColorScheme(
    primary = Color(0xFF36618E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF1A4975),
    inversePrimary = Color(0xFFA0CAFD),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF3B4858),
    tertiary = Color(0xFF00658E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC7E7FF),
    onTertiaryContainer = Color(0xFF004C6C),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF36618E),
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
    onErrorContainer = Color(0xFF93000A),
    primaryFixed = Color(0xFFD1E4FF),
    primaryFixedDim = Color(0xFFA0CAFD),
    onPrimaryFixed = Color(0xFF001D36),
    onPrimaryFixedVariant = Color(0xFF1A4975),
    secondaryFixed = Color(0xFFD7E3F8),
    secondaryFixedDim = Color(0xFFBBC7DB),
    onSecondaryFixed = Color(0xFF101C2B),
    onSecondaryFixedVariant = Color(0xFF3B4858),
    tertiaryFixed = Color(0xFFC7E7FF),
    tertiaryFixedDim = Color(0xFF84CFFF),
    onTertiaryFixed = Color(0xFF001E2E),
    onTertiaryFixedVariant = Color(0xFF004C6C),
)

private val MankeuDarkColors = darkColorScheme(
    primary = Color(0xFFA0CAFD),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1A4975),
    onPrimaryContainer = Color(0xFFD1E4FF),
    inversePrimary = Color(0xFF36618E),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFF84CFFF),
    onTertiary = Color(0xFF00344C),
    tertiaryContainer = Color(0xFF004C6C),
    onTertiaryContainer = Color(0xFFC7E7FF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceTint = Color(0xFFA0CAFD),
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
    primaryFixedDim = Color(0xFFA0CAFD),
    onPrimaryFixed = Color(0xFF001D36),
    onPrimaryFixedVariant = Color(0xFF1A4975),
    secondaryFixed = Color(0xFFD7E3F8),
    secondaryFixedDim = Color(0xFFBBC7DB),
    onSecondaryFixed = Color(0xFF101C2B),
    onSecondaryFixedVariant = Color(0xFF3B4858),
    tertiaryFixed = Color(0xFFC7E7FF),
    tertiaryFixedDim = Color(0xFF84CFFF),
    onTertiaryFixed = Color(0xFF001E2E),
    onTertiaryFixedVariant = Color(0xFF004C6C),
)

data class MankeuSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val chart: List<Color>,
)

private val MankeuLightSemanticColors = MankeuSemanticColors(
    success = Color(0xFF006B5A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF80F7DB),
    onSuccessContainer = Color(0xFF00201A),
    danger = Color(0xFF9A442D),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFFDBD2),
    onDangerContainer = Color(0xFF3C0800),
    warningContainer = Color(0xFFE1E991),
    onWarningContainer = Color(0xFF1A1D00),
    chart = listOf(
        Color(0xFF006D43),
        Color(0xFFB72122),
        Color(0xFF005FAD),
        Color(0xFF6D43C6),
        Color(0xFF00677F),
        Color(0xFFB4047E),
    ),
)

private val MankeuDarkSemanticColors = MankeuSemanticColors(
    success = Color(0xFF62DABF),
    onSuccess = Color(0xFF00382E),
    successContainer = Color(0xFF005143),
    onSuccessContainer = Color(0xFF80F7DB),
    danger = Color(0xFFFFB4A1),
    onDanger = Color(0xFF5D1805),
    dangerContainer = Color(0xFF7C2E19),
    onDangerContainer = Color(0xFFFFDBD2),
    warningContainer = Color(0xFF444B01),
    onWarningContainer = Color(0xFFE1E991),
    chart = listOf(
        Color(0xFF68CC94),
        Color(0xFFFF9F96),
        Color(0xFF8BBBFF),
        Color(0xFFC5ABFF),
        Color(0xFF6DC4E1),
        Color(0xFFFF98CE),
    ),
)

val LocalMankeuSemanticColors = androidx.compose.runtime.staticCompositionLocalOf { MankeuLightSemanticColors }

private val MankeuShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

data class MankeuMotion(
    val enabled: Boolean,
    val fastMillis: Int,
    val mediumMillis: Int,
) {
    companion object {
        val Full = MankeuMotion(enabled = true, fastMillis = 180, mediumMillis = 320)
        val Reduced = MankeuMotion(enabled = true, fastMillis = 90, mediumMillis = 140)
        val Off = MankeuMotion(enabled = false, fastMillis = 0, mediumMillis = 0)

        fun of(mode: MotionMode): MankeuMotion = when (mode) {
            MotionMode.FULL -> Full
            MotionMode.REDUCED -> Reduced
            MotionMode.OFF -> Off
        }

        fun resolve(mode: MotionMode, systemScale: Float): MankeuMotion = when {
            systemScale <= 0f -> Off
            systemScale < 1f -> of(mode).let { if (it == Full) Reduced else it }
            else -> of(mode)
        }
    }
}

val LocalMotion = androidx.compose.runtime.staticCompositionLocalOf { MankeuMotion.Full }

@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun MankeuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: MankeuMotion = MankeuMotion.Full,
    content: @Composable () -> Unit,
) {
    // Tokens are passed straight through.
    //
    // These used to be interpolated: 40 `ColorScheme` roles plus 11 semantic ones, each wrapped in
    // its own `animateColorAsState`, so ~51 animation state objects were created and read on the
    // very first composition and re-read on every recomposition of the whole tree — all to
    // cross-fade a theme switch most owners press once, if ever. The cost was paid on every launch
    // by everyone; the benefit went to a rare gesture. `Crossfade` on the tree is not the
    // alternative either (see D-017: it discards scroll position, half-filled forms and open
    // dialogs). A theme change is now an instant swap, which is what the platform itself does.
    val scheme = if (darkTheme) MankeuDarkColors else MankeuLightColors
    val semanticColors = if (darkTheme) MankeuDarkSemanticColors else MankeuLightSemanticColors
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMankeuSemanticColors provides semanticColors,
        LocalMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = MankeuShapes,
            content = content,
        )
    }
}

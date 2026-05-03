package com.sortoapps.chorepilot.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF5554B1),
    onPrimary = Color(0xFFFBF7FF),
    primaryContainer = Color(0xFF9594F6),
    onPrimaryContainer = Color(0xFF120670),
    secondary = Color(0xFF016C5C),
    onSecondary = Color(0xFFE3FFF6),
    secondaryContainer = Color(0xFF9FF2DE),
    onSecondaryContainer = Color(0xFF005D4F),
    tertiary = Color(0xFF34628C),
    onTertiary = Color(0xFFF7F9FF),
    tertiaryContainer = Color(0xFFA0CDFC),
    onTertiaryContainer = Color(0xFF0D456D),
    background = Color(0xFFF8F9FE),
    onBackground = Color(0xFF2D333A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2D333A),
    surfaceVariant = Color(0xFFDDE3EC),
    onSurfaceVariant = Color(0xFF5A6068),
    outline = Color(0xFFADB2BB),
    outlineVariant = Color(0xFFDDE3EC),
    error = Color(0xFFA8364B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8E8AFB),
    onPrimary = Color(0xFF181261),
    primaryContainer = Color(0xFF1F2547),
    onPrimaryContainer = Color(0xFFE4E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF063A35),
    secondaryContainer = Color(0xFF0F3E3A),
    onSecondaryContainer = Color(0xFFC6FFF6),
    tertiary = Color(0xFF8E8AFB),
    onTertiary = Color(0xFF181261),
    tertiaryContainer = Color(0xFF1F2547),
    onTertiaryContainer = Color(0xFFE4E7FF),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF233042),
    error = Color(0xFFF87171),
)

private val EditorialLightColors = lightColorScheme(
    primary = Color(0xFF9F402D),
    onPrimary = Color(0xFFFFFBF9),
    primaryContainer = Color(0xFFE2725B),
    onPrimaryContainer = Color(0xFF3E170F),
    secondary = Color(0xFF795900),
    onSecondary = Color(0xFFFFF8E8),
    secondaryContainer = Color(0xFFF5E5B7),
    onSecondaryContainer = Color(0xFF5A4300),
    tertiary = Color(0xFF42596B),
    onTertiary = Color(0xFFF7FAFD),
    tertiaryContainer = Color(0xFFD7E3EC),
    onTertiaryContainer = Color(0xFF253845),
    background = Color(0xFFFEF8F4),
    onBackground = Color(0xFF2F4F4F),
    surface = Color(0xFFFEF8F4),
    onSurface = Color(0xFF2F4F4F),
    surfaceVariant = Color(0xFFF2EDE8),
    onSurfaceVariant = Color(0xFF6F6964),
    outline = Color(0xFFDDC0BA),
    outlineVariant = Color(0xFFF2E6E1),
    error = Color(0xFFBA1A1A),
)

private val EditorialDarkColors = darkColorScheme(
    primary = Color(0xFFF0A28E),
    onPrimary = Color(0xFF542115),
    primaryContainer = Color(0xFF7C3526),
    onPrimaryContainer = Color(0xFFFFDBD2),
    secondary = Color(0xFFE3C77D),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFFFE9B2),
    tertiary = Color(0xFFB8CAD8),
    onTertiary = Color(0xFF213643),
    tertiaryContainer = Color(0xFF394F5D),
    onTertiaryContainer = Color(0xFFD7E3EC),
    background = Color(0xFF191412),
    onBackground = Color(0xFFF3E9E2),
    surface = Color(0xFF211B18),
    onSurface = Color(0xFFF3E9E2),
    surfaceVariant = Color(0xFF2C2521),
    onSurfaceVariant = Color(0xFFD4C0B7),
    outline = Color(0xFF8F766D),
    outlineVariant = Color(0xFF473C37),
    error = Color(0xFFFFB4AB),
)

private val SunDrenchedLightColors = lightColorScheme(
    primary = Color(0xFF944A00),
    onPrimary = Color(0xFFFFFAF7),
    primaryContainer = Color(0xFFFF9238),
    onPrimaryContainer = Color(0xFF3A1700),
    secondary = Color(0xFFFC7127),
    onSecondary = Color(0xFFFFFAF7),
    secondaryContainer = Color(0xFFFFD4BD),
    onSecondaryContainer = Color(0xFF4A1E06),
    tertiary = Color(0xFF77574D),
    onTertiary = Color(0xFFFFF8F6),
    tertiaryContainer = Color(0xFFF1DDD3),
    onTertiaryContainer = Color(0xFF38251F),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF2B1611),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF2B1611),
    surfaceVariant = Color(0xFFFFF0ED),
    onSurfaceVariant = Color(0xFF6A5148),
    outline = Color(0xFFD9C2B3),
    outlineVariant = Color(0xFFF3DFD7),
    error = Color(0xFFB3261E),
)

private val SunDrenchedDarkColors = darkColorScheme(
    primary = Color(0xFFFFB774),
    onPrimary = Color(0xFF4B2500),
    primaryContainer = Color(0xFF7A3F00),
    onPrimaryContainer = Color(0xFFFFDDBF),
    secondary = Color(0xFFFFA463),
    onSecondary = Color(0xFF552003),
    secondaryContainer = Color(0xFF7A320C),
    onSecondaryContainer = Color(0xFFFFDCCB),
    tertiary = Color(0xFFD5B7AC),
    onTertiary = Color(0xFF3F2A23),
    tertiaryContainer = Color(0xFF5A4037),
    onTertiaryContainer = Color(0xFFF4DFD6),
    background = Color(0xFF1B110D),
    onBackground = Color(0xFFF5E8E1),
    surface = Color(0xFF241814),
    onSurface = Color(0xFFF5E8E1),
    surfaceVariant = Color(0xFF32231E),
    onSurfaceVariant = Color(0xFFE0C7BD),
    outline = Color(0xFF9D7F74),
    outlineVariant = Color(0xFF4A3933),
    error = Color(0xFFFFB4AB),
)

private val ChorePilotTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold),
)

private val EditorialTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.35).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
)

private val ChorePilotShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

private val EditorialShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(40.dp),
)

@Immutable
data class ChorePilotSpacing(
    val xSmall: androidx.compose.ui.unit.Dp = 8.dp,
    val small: androidx.compose.ui.unit.Dp = 16.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val mediumLarge: androidx.compose.ui.unit.Dp = 24.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val xLarge: androidx.compose.ui.unit.Dp = 32.dp,
    val xxLarge: androidx.compose.ui.unit.Dp = 40.dp,
)

val LocalChorePilotSpacing = staticCompositionLocalOf { ChorePilotSpacing() }
val LocalAppearancePreset = staticCompositionLocalOf { AppearancePreset.Classic }

object ChorePilotThemeTokens {
    val spacing: ChorePilotSpacing
        @Composable get() = LocalChorePilotSpacing.current

    val appearancePreset: AppearancePreset
        @Composable get() = LocalAppearancePreset.current
}

@Composable
fun ChorePilotTheme(
    themeMode: ThemeMode = ThemeMode.Light,
    appearancePreset: AppearancePreset = AppearancePreset.Classic,
    largerTextEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    } || (themeMode == ThemeMode.Dark && isSystemInDarkTheme())
    val usesEditorialSystem = appearancePreset != AppearancePreset.Classic
    val typography = when {
        usesEditorialSystem && largerTextEnabled -> EditorialTypography.scaled(1.08f)
        usesEditorialSystem -> EditorialTypography
        largerTextEnabled -> ChorePilotTypography.scaled(1.08f)
        else -> ChorePilotTypography
    }
    val colorScheme = when (appearancePreset) {
        AppearancePreset.Classic -> if (darkTheme) DarkColors else LightColors
        AppearancePreset.EditorialWarm -> if (darkTheme) EditorialDarkColors else EditorialLightColors
        AppearancePreset.SunDrenchedEditorial -> if (darkTheme) SunDrenchedDarkColors else SunDrenchedLightColors
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppearancePreset provides appearancePreset,
        LocalChorePilotSpacing provides ChorePilotSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = if (usesEditorialSystem) EditorialShapes else ChorePilotShapes,
            content = content,
        )
    }
}

private fun Typography.scaled(scale: Float): Typography = copy(
    displayLarge = displayLarge.scaled(scale),
    displayMedium = displayMedium.scaled(scale),
    displaySmall = displaySmall.scaled(scale),
    headlineLarge = headlineLarge.scaled(scale),
    headlineMedium = headlineMedium.scaled(scale),
    headlineSmall = headlineSmall.scaled(scale),
    titleLarge = titleLarge.scaled(scale),
    titleMedium = titleMedium.scaled(scale),
    titleSmall = titleSmall.scaled(scale),
    bodyLarge = bodyLarge.scaled(scale),
    bodyMedium = bodyMedium.scaled(scale),
    bodySmall = bodySmall.scaled(scale),
    labelLarge = labelLarge.scaled(scale),
    labelMedium = labelMedium.scaled(scale),
    labelSmall = labelSmall.scaled(scale),
)

private fun TextStyle.scaled(scale: Float): TextStyle = copy(
    fontSize = fontSize.scaled(scale),
    lineHeight = lineHeight.scaled(scale),
)

private fun TextUnit.scaled(scale: Float): TextUnit = if (this != TextUnit.Unspecified) this * scale else this

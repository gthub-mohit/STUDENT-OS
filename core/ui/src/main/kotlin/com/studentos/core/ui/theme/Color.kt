package com.studentos.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Linear/Raycast Zinc & Electric Violet Design Tokens ────────────────────────

// Core Palette
val ZincAppBackground = Color(0xFF09090B)
val ZincCardSurface = Color(0xFF121215)
val ZincCardBorder = Color(0xFF27272A)
val ZincInnerSurface = Color(0xFF18181B)
val ZincInnerBorder = Color(0xFF2E2E34)

// Violet Accent
val VioletPrimary = Color(0xFF7C3AED)      // Primary CTA
val VioletBright = Color(0xFFA78BFA)       // Bright accent
val VioletBadgeBg = Color(0xFF2E1065)      // Soft violet badge background
val VioletBorder = Color(0xFF4C1D95)

// Typography Neutrals
val ZincTextPrimary = Color(0xFFFAFAFA)
val ZincTextSecondary = Color(0xFFA1A1AA)
val ZincTextMuted = Color(0xFF71717A)

// Status & Semantic
val SuccessText = Color(0xFF34D399)
val SuccessBg = Color(0xFF064E3B)
val DestructiveRed = Color(0xFFF87171)
val DestructiveBg = Color(0xFF450A0A)
val DestructiveBorder = Color(0xFF7F1D1D)

val WarningAmber = Color(0xFFFBBF24)
val WarningBg = Color(0xFF78350F)

val InfoBlue = Color(0xFF60A5FA)
val InfoBg = Color(0xFF1E3A8A)

// Priority Tokens
val PriorityHighText = Color(0xFFF87171)
val PriorityHighBg = Color(0xFF450A0A)
val PriorityMediumText = Color(0xFFFBBF24)
val PriorityMediumBg = Color(0xFF78350F)
val PriorityLowText = Color(0xFFA1A1AA)
val PriorityLowBg = Color(0xFF18181B)

@Immutable
data class StudentOsColors(
    val appBackground: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val innerSurface: Color,
    val innerBorder: Color,
    val primaryCta: Color,
    val brightAccent: Color,
    val badgeVioletBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val successText: Color,
    val successBg: Color,
    val destructiveText: Color,
    val destructiveBg: Color,
    val destructiveBorder: Color,
    val priorityHighText: Color,
    val priorityHighBg: Color,
    val priorityMediumText: Color,
    val priorityMediumBg: Color,
    val priorityLowText: Color,
    val priorityLowBg: Color
) {
    val violetBadgeBg: Color get() = badgeVioletBg
    val primaryText: Color get() = textPrimary
    val secondaryText: Color get() = textSecondary
    val mutedText: Color get() = textMuted
    val destructiveRed: Color get() = destructiveText
}

val DarkStudentOsColors = StudentOsColors(
    appBackground = ZincAppBackground,
    cardSurface = ZincCardSurface,
    cardBorder = ZincCardBorder,
    innerSurface = ZincInnerSurface,
    innerBorder = ZincInnerBorder,
    primaryCta = VioletPrimary,
    brightAccent = VioletBright,
    badgeVioletBg = VioletBadgeBg,
    textPrimary = ZincTextPrimary,
    textSecondary = ZincTextSecondary,
    textMuted = ZincTextMuted,
    successText = SuccessText,
    successBg = SuccessBg,
    destructiveText = DestructiveRed,
    destructiveBg = DestructiveBg,
    destructiveBorder = DestructiveBorder,
    priorityHighText = PriorityHighText,
    priorityHighBg = PriorityHighBg,
    priorityMediumText = PriorityMediumText,
    priorityMediumBg = PriorityMediumBg,
    priorityLowText = PriorityLowText,
    priorityLowBg = PriorityLowBg
)

val LocalStudentOsColors = staticCompositionLocalOf { DarkStudentOsColors }

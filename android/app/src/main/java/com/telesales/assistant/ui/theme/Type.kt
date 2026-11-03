package com.telesales.assistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BaseTypography = Typography()

val Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(lineHeight = 64.sp),
    displayMedium = BaseTypography.displayMedium.copy(lineHeight = 54.sp),
    displaySmall = BaseTypography.displaySmall.copy(lineHeight = 46.sp),
    headlineLarge = BaseTypography.headlineLarge.copy(lineHeight = 42.sp),
    headlineMedium = BaseTypography.headlineMedium.copy(lineHeight = 36.sp),
    headlineSmall = BaseTypography.headlineSmall.copy(lineHeight = 32.sp),
    titleLarge = BaseTypography.titleLarge.copy(lineHeight = 30.sp),
    titleMedium = BaseTypography.titleMedium.copy(lineHeight = 26.sp),
    titleSmall = BaseTypography.titleSmall.copy(lineHeight = 22.sp),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = BaseTypography.bodyMedium.copy(lineHeight = 22.sp),
    bodySmall = BaseTypography.bodySmall.copy(lineHeight = 19.sp),
    labelLarge = BaseTypography.labelLarge.copy(lineHeight = 21.sp),
    labelMedium = BaseTypography.labelMedium.copy(lineHeight = 18.sp),
    labelSmall = BaseTypography.labelSmall.copy(lineHeight = 16.sp),
)


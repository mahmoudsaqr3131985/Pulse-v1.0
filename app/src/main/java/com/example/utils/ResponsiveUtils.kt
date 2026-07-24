package com.example.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ScreenSizeCategory {
    COMPACT_PHONE,  // Small phone (<360dp width)
    STANDARD_PHONE, // Standard phone (360dp - 600dp)
    TABLET          // Wide device (>600dp)
}

object ResponsiveUtils {
    @Composable
    fun getScreenCategory(): ScreenSizeCategory {
        val configuration = LocalConfiguration.current
        val width = configuration.screenWidthDp.dp
        return when {
            width < 360.dp -> ScreenSizeCategory.COMPACT_PHONE
            width <= 600.dp -> ScreenSizeCategory.STANDARD_PHONE
            else -> ScreenSizeCategory.TABLET
        }
    }

    @Composable
    fun responsivePadding(): Dp {
        return when (getScreenCategory()) {
            ScreenSizeCategory.COMPACT_PHONE -> 12.dp
            ScreenSizeCategory.STANDARD_PHONE -> 20.dp
            ScreenSizeCategory.TABLET -> 32.dp
        }
    }
}

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedAccent,
    onPrimary = SophisticatedBg,
    primaryContainer = SophisticatedSurfaceVariant,
    onPrimaryContainer = SophisticatedText,
    secondary = SophisticatedSubText,
    onSecondary = SophisticatedBg,
    tertiary = Pink80,
    background = SophisticatedBg,
    onBackground = SophisticatedText,
    surface = SophisticatedSurface,
    onSurface = SophisticatedText,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedSubText,
    outline = SophisticatedBorder,
    outlineVariant = SophisticatedBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = InfinityBlue,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = InfinityLightBg,
    surface = InfinitySurfaceLight
  )

@Composable
fun InfinityBrowserTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = InfinityBrowserTheme(darkTheme, dynamicColor, content)

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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),          // GeometricPurple
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),    // GeometricLightPurple
    onPrimaryContainer = Color(0xFF21005D), // GeometricDarkPurple
    secondary = Color(0xFFD3E3FD),        // GeometricLightBlue
    onSecondary = Color(0xFF041E49),      // GeometricDarkBlue
    background = Color(0xFFFEF7FF),       // GeometricCanvasBg
    onBackground = Color(0xFF1D1B20),     // GeometricDarkBody
    surface = Color(0xFFFEF7FF),          // GeometricCanvasBg
    onSurface = Color(0xFF1D1B20),        // GeometricDarkBody
    surfaceVariant = Color(0xFFF3EDF7),    // GeometricInputBg
    onSurfaceVariant = Color(0xFF49454F),  // GeometricGreyText
    outlineVariant = Color(0xFFE7E0EC)     // GeometricLavendarBg
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Use our specific custom theme colors
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

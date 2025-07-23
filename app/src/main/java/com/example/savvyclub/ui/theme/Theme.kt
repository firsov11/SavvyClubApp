package com.example.savvyclub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9DE1D3),           // Мята (основной цвет)
    onPrimary = Color.Black,

    secondary = Color(0xFFF6D5D0),         // Пудра (мягкий акцент)
    onSecondary = Color.Black,

    tertiary = Color(0xFFE5B8B7),          // Розовато-песочный
    onTertiary = Color.Black,

    primaryContainer = Color(0xFFCCEDE5),  // Светлая мятная подложка
    onPrimaryContainer = Color.Black,

    background = Color(0xFFFFFFFF),        // Белый фон
    onBackground = Color(0xFF1A1A1A),      // Тёмный текст

    surface = Color(0xFFFDFDFC),           // Почти белая поверхность
    onSurface = Color(0xFF2A2A2A),

    surfaceVariant = Color(0xFFF1F3F2),    // Для карточек, списков
    onSurfaceVariant = Color(0xFF4A4A4A),

    error = Color(0xFFB00020),             // Материал error
    onError = Color.White,

    outline = Color(0xFFB0BEC5),           // Светло-серый для бордеров
    inversePrimary = Color(0xFF3E8E81)     // Тёмная мята для hover, dark mode
)


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2E3B2F),           // SwampDark — основной болотный
    onPrimary = Color.White,

    secondary = Color(0xFF556B2F),         // OliveDark
    onSecondary = Color.White,

    tertiary = Color(0xFF6E7F4F),          // BrownGreen
    onTertiary = Color.White,

    primaryContainer = Color(0xFFFFA726), // Accent — тёплый оранжевый
    onPrimaryContainer = Color.Black,

    background = Color(0xFF1A1F1A),        // Темный фон
    onBackground = Color(0xFFEFEFEF),      // Чуть мягче, чем белый

    surface = Color(0xFF232B23),          // Поверхность
    onSurface = Color(0xFFEFEFEF),

    surfaceVariant = Color(0xFF2F3A2F),    // Для карточек / списков
    onSurfaceVariant = Color(0xFFBBBBBB),

    error = Color(0xFFCF6679),            // Material error
    onError = Color.Black,

    outline = Color(0xFFFFFFFF).copy(alpha = 0.2f), // для бордеров
    inversePrimary = Color(0xFF80CBC4)    // Альтернативный холодный акцент
)




@Composable
fun SavvyClubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
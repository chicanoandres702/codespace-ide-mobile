package com.codespace.ide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Token colors used by the syntax highlighter, theme-aware. */
data class EditorColors(
    val background: Color,
    val gutter: Color,
    val text: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val function: Color,
    val type: Color,
    val variable: Color,
    val operator: Color,
    val selection: Color,
    val currentLine: Color,
)

val LocalEditorColors = staticCompositionLocalOf {
    DarkEditorColors
}

// Catppuccin-inspired dark IDE palette
val DarkEditorColors = EditorColors(
    background = Color(0xFF1E1E2E),
    gutter = Color(0xFF6C7086),
    text = Color(0xFFCDD6F4),
    keyword = Color(0xFFCBA6F7),
    string = Color(0xFFA6E3A1),
    number = Color(0xFFFAB387),
    comment = Color(0xFF6C7086),
    function = Color(0xFF89B4FA),
    type = Color(0xFFF9E2AF),
    variable = Color(0xFFF38BA8),
    operator = Color(0xFF89DCEB),
    selection = Color(0x4089B4FA),
    currentLine = Color(0xFF313244),
)

val LightEditorColors = EditorColors(
    background = Color(0xFFFAFAFA),
    gutter = Color(0xFF9CA0B0),
    text = Color(0xFF4C4F69),
    keyword = Color(0xFF8839EF),
    string = Color(0xFF40A02B),
    number = Color(0xFFFE640B),
    comment = Color(0xFF9CA0B0),
    function = Color(0xFF1E66F5),
    type = Color(0xFFDF8E1D),
    variable = Color(0xFFD20F39),
    operator = Color(0xFF179299),
    selection = Color(0x401E66F5),
    currentLine = Color(0xFFE6E9EF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89B4FA),
    onPrimary = Color(0xFF11111B),
    background = Color(0xFF1E1E2E),
    surface = Color(0xFF181825),
    onBackground = Color(0xFFCDD6F4),
    onSurface = Color(0xFFCDD6F4),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E66F5),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF4C4F69),
    onSurface = Color(0xFF4C4F69),
)

@Composable
fun CodeSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val editorColors = if (darkTheme) DarkEditorColors else LightEditorColors
    CompositionLocalProvider(LocalEditorColors provides editorColors) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

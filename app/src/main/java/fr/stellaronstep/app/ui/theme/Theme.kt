package fr.stellaronstep.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF02060A)
val Surface = Color(0xFF08111B)
val SurfaceRaised = Color(0xFF0E1A28)

val Accent = Color(0xFF36B9FF)
val AccentStrong = Color(0xFF168DFF)
val AccentOrange = Color(0xFFFF9D22)

val Text = Color(0xFFF5FAFF)
val Muted = Color(0xFF93A8BF)
val Danger = Color(0xFFFF5F63)
val Success = Color(0xFF49E875)

private val Colors = darkColorScheme(
    primary = Accent,
    secondary = AccentOrange,
    background = Bg,
    surface = Surface,
    surfaceVariant = SurfaceRaised,
    onPrimary = Bg,
    onSecondary = Bg,
    onBackground = Text,
    onSurface = Text,
    onSurfaceVariant = Text,
    error = Danger
)

@Composable
fun StellarOnStepTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = Colors,
        content = content
    )
}
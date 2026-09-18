package fr.onsteppilot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0A0D12)
val Surface = Color(0xFF121821)
val SurfaceRaised = Color(0xFF1A2230)
val Accent = Color(0xFFFFA43A)
val Text = Color(0xFFF4F7FA)
val Muted = Color(0xFF9BA7B4)
val Danger = Color(0xFFFF5C5C)

private val Colors = darkColorScheme(
    primary = Accent,
    background = Bg,
    surface = Surface,
    onPrimary = Bg,
    onBackground = Text,
    onSurface = Text
)

@Composable
fun OnStepPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

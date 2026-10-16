package ly.jawdat.alhaya.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ly.jawdat.alhaya.R

// Brand colours from the جودة الحياة logo: its teal calligraphy and leaves, the light-green ring,
// on a warm cream, with a date brown for production costs.
val Teal = Color(0xFF3F6F7C)
val Leaf = Color(0xFF7FAE45)
val DateBrown = Color(0xFF8A5A33)
val Cream = Color(0xFFFAF7F0)
val Danger = Color(0xFFB3261E)
val Warn = Color(0xFFB26A00)
val Good = Color(0xFF2E7D32)

private val light = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E7EC),
    onPrimaryContainer = Color(0xFF0B2A32),
    secondary = DateBrown,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3DFC9),
    onSecondaryContainer = Color(0xFF2E1A09),
    tertiary = Leaf,
    background = Cream,
    onBackground = Color(0xFF1C1C17),
    surface = Cream,
    onSurface = Color(0xFF1C1C17),
    surfaceContainer = Color(0xFFF1EDE3),
    surfaceContainerLow = Color(0xFFF6F2EA),
    surfaceContainerHigh = Color(0xFFECE7DC),
    surfaceVariant = Color(0xFFE4E1D6),
    onSurfaceVariant = Color(0xFF47473E),
    outline = Color(0xFF78786D),
    outlineVariant = Color(0xFFC9C7BB),
    error = Danger,
)

private val dark = darkColorScheme(
    primary = Color(0xFF9DCFDB),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF244E59),
    onPrimaryContainer = Color(0xFFD4E7EC),
    secondary = Color(0xFFE7BE94),
    onSecondary = Color(0xFF442A0F),
    secondaryContainer = Color(0xFF5E4026),
    onSecondaryContainer = Color(0xFFF3DFC9),
    tertiary = Color(0xFFB5D88B),
    background = Color(0xFF141A1B),
    onBackground = Color(0xFFE3E3DA),
    surface = Color(0xFF151A16),
    onSurface = Color(0xFFE3E3DA),
    surfaceContainer = Color(0xFF1F2620),
    surfaceContainerLow = Color(0xFF1A201B),
    surfaceContainerHigh = Color(0xFF29302A),
    surfaceVariant = Color(0xFF3A4039),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF434940),
)

private val Readex = FontFamily(Font(R.font.readex_pro))

private fun Typography.withFont(f: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = f)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f().copy(fontWeight = FontWeight.SemiBold), titleMedium = titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f().copy(fontSize = 16.sp), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

@Composable
fun JawdatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) dark else light,
        typography = Typography().withFont(Readex),
        content = content,
    )
}

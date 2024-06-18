package xyz.aprildown.timer.app.base.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.palettes.CorePalette

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dynamicTheme = remember { newDynamicTheme }
    MaterialTheme(
        colorScheme = remember(
            isDarkTheme,
            dynamicTheme.colorPrimary,
            dynamicTheme.colorSecondary,
        ) {
            val core = CorePalette.of(dynamicTheme.colorPrimary)
            val secondary = CorePalette.of(dynamicTheme.colorSecondary)
            if (isDarkTheme) {
                darkFromCorePalette(core = core, secondary = secondary)
            } else {
                lightFromCorePalette(core = core, secondary = secondary)
            }
        },
        content = content,
    )
}

/** [com.materialkolor.scheme.Scheme]
 *  [androidx.compose.material3.lightColorScheme]
 * */
private fun lightFromCorePalette(core: CorePalette, secondary: CorePalette = core): ColorScheme {
    return ColorScheme(
        primary = Color(core.a1.tone(40)),
        onPrimary = Color(core.a1.tone(100)),
        primaryContainer = Color(core.a1.tone(90)),
        onPrimaryContainer = Color(core.a1.tone(10)),
        inversePrimary = Color(core.a1.tone(80)),

        secondary = Color(secondary.a1.tone(40)),
        onSecondary = Color(secondary.a1.tone(100)),
        secondaryContainer = Color(secondary.a1.tone(90)),
        onSecondaryContainer = Color(secondary.a1.tone(10)),

        tertiary = Color(core.a3.tone(40)),
        onTertiary = Color(core.a3.tone(100)),
        tertiaryContainer = Color(core.a3.tone(90)),
        onTertiaryContainer = Color(core.a3.tone(10)),
        background = Color(core.n1.tone(99)),
        onBackground = Color(core.n1.tone(10)),
        surface = Color(core.n1.tone(99)),
        onSurface = Color(core.n1.tone(10)),
        surfaceVariant = Color(core.n2.tone(90)),
        onSurfaceVariant = Color(core.n2.tone(30)),
        surfaceTint = Color(core.a1.tone(40)),
        inverseSurface = Color(core.n1.tone(20)),
        inverseOnSurface = Color(core.n1.tone(95)),
        error = Color(core.error.tone(40)),
        onError = Color(core.error.tone(100)),
        errorContainer = Color(core.error.tone(90)),
        onErrorContainer = Color(core.error.tone(10)),
        outline = Color(core.n2.tone(50)),
        outlineVariant = Color(core.n2.tone(80)),
        scrim = Color(core.n1.tone(0)),
    )
}

private fun darkFromCorePalette(core: CorePalette, secondary: CorePalette = core): ColorScheme {
    return ColorScheme(
        primary = Color(core.a1.tone(80)),
        onPrimary = Color(core.a1.tone(20)),
        primaryContainer = Color(core.a1.tone(30)),
        onPrimaryContainer = Color(core.a1.tone(90)),
        inversePrimary = Color(core.a1.tone(40)),

        secondary = Color(secondary.a1.tone(80)),
        onSecondary = Color(secondary.a1.tone(20)),
        secondaryContainer = Color(secondary.a1.tone(30)),
        onSecondaryContainer = Color(secondary.a1.tone(90)),

        tertiary = Color(core.a3.tone(80)),
        onTertiary = Color(core.a3.tone(20)),
        tertiaryContainer = Color(core.a3.tone(30)),
        onTertiaryContainer = Color(core.a3.tone(90)),
        background = Color(core.n1.tone(10)),
        onBackground = Color(core.n1.tone(90)),
        surface = Color(core.n1.tone(10)),
        onSurface = Color(core.n1.tone(90)),
        surfaceVariant = Color(core.n2.tone(30)),
        onSurfaceVariant = Color(core.n2.tone(80)),
        surfaceTint = Color(core.a1.tone(80)),
        inverseSurface = Color(core.n1.tone(90)),
        inverseOnSurface = Color(core.n1.tone(20)),
        error = Color(core.error.tone(80)),
        onError = Color(core.error.tone(20)),
        errorContainer = Color(core.error.tone(30)),
        onErrorContainer = Color(core.error.tone(80)),
        outline = Color(core.n2.tone(60)),
        outlineVariant = Color(core.n2.tone(30)),
        scrim = Color(core.n1.tone(0)),
    )
}

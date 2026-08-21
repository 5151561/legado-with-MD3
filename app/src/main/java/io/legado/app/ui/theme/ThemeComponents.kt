package io.legado.app.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.LruCache
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.legado.app.core.designsystem.theme.ProvideLegadoTheme

@Composable
fun rememberCustomFont(fontPath: String?): FontFamily? {
    val context = LocalContext.current.applicationContext
    val path = fontPath?.takeIf(String::isNotBlank)
    val cachedFont = remember(path) {
        path?.let { synchronized(customFontCache) { customFontCache.get(it) } }
    }
    // 加载结果跨 path 保留：切换字体时旧字体一直用到新字体就位，
    // 中途不回落默认字体。
    var loadedFont by remember(context) { mutableStateOf<FontFamily?>(null) }
    LaunchedEffect(path, context) {
        if (path == null || cachedFont != null) return@LaunchedEffect
        loadedFont = withContext(Dispatchers.IO) {
            loadCustomFont(context, path)?.also {
                synchronized(customFontCache) { customFontCache.put(path, it) }
            }
        }
    }
    // path 为空即"清除"，必须当帧生效，不能受 loadedFont 影响。
    return if (path == null) null else cachedFont ?: loadedFont
}

private val customFontCache = LruCache<String, FontFamily>(4)

private fun loadCustomFont(context: Context, fontPath: String): FontFamily? =
    runCatching {
        val uri = Uri.parse(fontPath)
        val typeface = if (uri.scheme == "content") {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                Typeface.Builder(it.fileDescriptor).build()
            }
        } else {
            Typeface.createFromFile(uri.path)
        }
        typeface?.let(::FontFamily)
    }.getOrNull()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialThemeWrapper(
    themeColors: LegadoThemeMode,
    customFontFamily: FontFamily?,
    content: @Composable () -> Unit
) {
    val themeSettings = LocalAppUiConfiguration.current.theme
    val darkTheme = themeColors.isDark
    val colorScheme = themeColors.colorScheme
    
    val materialTypography = remember(customFontFamily) {
        val base = Typography()
        if (customFontFamily != null) {
            base.copy(
                headlineLarge = base.headlineLarge.copy(fontFamily = customFontFamily),
                headlineMedium = base.headlineMedium.copy(fontFamily = customFontFamily),
                headlineSmall = base.headlineSmall.copy(fontFamily = customFontFamily),
                titleLarge = base.titleLarge.copy(fontFamily = customFontFamily),
                titleMedium = base.titleMedium.copy(fontFamily = customFontFamily),
                titleSmall = base.titleSmall.copy(fontFamily = customFontFamily),
                bodyLarge = base.bodyLarge.copy(fontFamily = customFontFamily),
                bodyMedium = base.bodyMedium.copy(fontFamily = customFontFamily),
                bodySmall = base.bodySmall.copy(fontFamily = customFontFamily),
                labelLarge = base.labelLarge.copy(fontFamily = customFontFamily),
                labelMedium = base.labelMedium.copy(fontFamily = customFontFamily),
                labelSmall = base.labelSmall.copy(fontFamily = customFontFamily)
            )
        } else {
            base
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = materialTypography,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes()
    ) {
        val legadoTypography = remember(materialTypography, customFontFamily) {
            materialTypography.toLegadoTypography().withFont(customFontFamily)
        }
        val surfaceInput = themeSettings.bookInfoInputColor
            .takeIf { it != 0 }
            ?.let(::Color)
            ?: Color.Unspecified
        val semanticColors = remember(colorScheme, surfaceInput) {
            colorScheme.toLegadoColorScheme(
                customBgColor = colorScheme.background,
                customFontColor = colorScheme.onSurface,
                customTopBarColor = colorScheme.surface,
                customNavBarColor = colorScheme.surface,
                surfaceInput = surfaceInput,
            )
        }

        ProvideLegadoTheme(
            mode = themeColors,
            colorScheme = semanticColors,
            typography = legadoTypography,
        ) {
            AppBackground(darkTheme = darkTheme) { content() }
        }
    }
}

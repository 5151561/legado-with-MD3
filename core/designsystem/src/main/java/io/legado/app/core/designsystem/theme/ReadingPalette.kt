package io.legado.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 正文阅读的纸色与字色。
 *
 * 设计稿画板 N-04 明确："夜墨方案下阅读页另有独立纸色，可在阅读样式抽屉里单独选择，
 * 不受此处影响。" 因此本调色板**独立于 [AppColorScheme]**，不随 App 主题联动，
 * 由阅读样式抽屉（画板 R-02b「字体与色彩」）单独选择并持久化。
 *
 * [inkDim] 用于页眉页脚信息栏等次级文字（画板 R-02c 的六格位）。
 */
@Immutable
data class ReadingPalette(
    val id: String,
    val paper: Color,
    val ink: Color,
    val inkDim: Color,
)

/** 纸——设计稿默认。 */
val ReadingPaperDefault = ReadingPalette(
    id = "paper",
    paper = Color(0xFFF4EEE3),
    ink = Color(0xFF2B2823),
    inkDim = Color(0xFF5E5951),
)

/** 白。 */
val ReadingPaperWhite = ReadingPalette(
    id = "white",
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF1B1B1B),
    inkDim = Color(0xFF5A5A5A),
)

/** 绿。 */
val ReadingPaperGreen = ReadingPalette(
    id = "green",
    paper = Color(0xFFE6EFE7),
    ink = Color(0xFF22302A),
    inkDim = Color(0xFF54615A),
)

/** 夜。 */
val ReadingPaperNight = ReadingPalette(
    id = "night",
    paper = Color(0xFF14181A),
    ink = Color(0xFFC6C3BC),
    inkDim = Color(0xFF8A8880),
)

/**
 * 纯黑正文背景，设置项「正文纯黑背景」开启时使用（画板 N-04：OLED 省电，对比更强）。
 * 它是 [ReadingPaperNight] 的变体，不作为独立纸色选项出现在抽屉里。
 */
val ReadingPaperBlack = ReadingPalette(
    id = "black",
    paper = Color(0xFF000000),
    ink = Color(0xFFC6C3BC),
    inkDim = Color(0xFF8A8880),
)

/** 抽屉中可选的纸色，顺序与画板 R-02b 一致；「背景图」是另一条分支，不在此列。 */
val ReadingPalettePresets = listOf(
    ReadingPaperDefault,
    ReadingPaperWhite,
    ReadingPaperGreen,
    ReadingPaperNight,
)

/**
 * 墨水屏模式：黑白高对比、无翻页动画（画板 R-02b）。
 * 动画开关由阅读器渲染层消费，此处只提供配色。
 */
val ReadingPaperEInk = ReadingPalette(
    id = "eink",
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF000000),
    inkDim = Color(0xFF000000),
)

val LocalReadingPalette = staticCompositionLocalOf { ReadingPaperDefault }

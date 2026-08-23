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
 * [inkDim] 用于页眉页脚信息栏等次级文字（画板 R-02c 的六格位），
 * [inkFaint] 再降一级，用于状态与计数（画板 S-06a 的「已读」「未缓存」）。
 *
 * 阅读面上的浮层（阅读器内目录、选中文本菜单）不能退回 App 的 surface 色阶——
 * 那会在正文之上开出一块与纸张无关的界面。因此纸色自带三个容器角色：
 * [paperHigh] 抬起的面板、[paperHighlight] 当前项底色、[paperOutline] 发丝线与分隔。
 *
 * 设计稿只给出「纸」这一档的全部七个值（`:root` 的 `--paper` 系列），
 * 其余纸色的后四个角色按同一关系推导，逐行标注「推导值」；
 * 后续设计若补正式值，替换那几行即可。
 */
@Immutable
data class ReadingPalette(
    val id: String,
    val paper: Color,
    val ink: Color,
    val inkDim: Color,
    val inkFaint: Color,
    val paperHigh: Color,
    val paperHighlight: Color,
    val paperOutline: Color,
)

/** 纸——设计稿默认。七个值全部来自设计稿。 */
val ReadingPaperDefault = ReadingPalette(
    id = "paper",
    paper = Color(0xFFF4EEE3),
    ink = Color(0xFF2B2823),
    inkDim = Color(0xFF5E5951),
    inkFaint = Color(0xFF8A8378),
    paperHigh = Color(0xFFFBF7F0),
    paperHighlight = Color(0xFFEFE7D9),
    paperOutline = Color(0xFFD9D0C2),
)

/** 白。 */
val ReadingPaperWhite = ReadingPalette(
    id = "white",
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF1B1B1B),
    inkDim = Color(0xFF5A5A5A),
    inkFaint = Color(0xFF8A8A8A), // 推导值
    paperHigh = Color(0xFFFFFFFF), // 推导值：已是最亮，面板与纸同色，靠 paperOutline 分界
    paperHighlight = Color(0xFFF2F2F2), // 推导值
    paperOutline = Color(0xFFE3E3E3), // 推导值
)

/** 绿。 */
val ReadingPaperGreen = ReadingPalette(
    id = "green",
    paper = Color(0xFFE6EFE7),
    ink = Color(0xFF22302A),
    inkDim = Color(0xFF54615A),
    inkFaint = Color(0xFF83908A), // 推导值
    paperHigh = Color(0xFFEFF6F0), // 推导值
    paperHighlight = Color(0xFFDDE8DF), // 推导值
    paperOutline = Color(0xFFCBD6CD), // 推导值
)

/** 夜。深色纸上「抬起」是提亮，与浅色纸方向相反。 */
val ReadingPaperNight = ReadingPalette(
    id = "night",
    paper = Color(0xFF14181A),
    ink = Color(0xFFC6C3BC),
    inkDim = Color(0xFF8A8880),
    inkFaint = Color(0xFF6A6862), // 推导值
    paperHigh = Color(0xFF1C2124), // 推导值
    paperHighlight = Color(0xFF232A2D), // 推导值
    paperOutline = Color(0xFF2C3236), // 推导值
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
    inkFaint = Color(0xFF6A6862), // 推导值
    paperHigh = Color(0xFF0B0B0B), // 推导值
    paperHighlight = Color(0xFF141414), // 推导值
    paperOutline = Color(0xFF262626), // 推导值
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
    // 墨水屏只有黑白两级：次级信息靠字号与位置区分，不靠灰阶——中间调在电子墨水上会抖动。
    inkFaint = Color(0xFF000000),
    paperHigh = Color(0xFFFFFFFF),
    paperHighlight = Color(0xFFFFFFFF),
    paperOutline = Color(0xFF000000),
)

val LocalReadingPalette = staticCompositionLocalOf { ReadingPaperDefault }

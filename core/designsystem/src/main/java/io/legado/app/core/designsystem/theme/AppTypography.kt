package io.legado.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 重设计的字阶。
 *
 * 角色与数值来自设计稿三面画板墙的实测统计，不是套用完整 MD3 字阶——设计只用到约十档，
 * 这里就只定义这十档，避免造出无人消费的空角色。
 *
 * 字族：设计稿用 `Noto Serif SC` 与 `Noto Sans SC`。仓库未打包这两个字体，
 * 因此衬线回退到 [FontFamily.Serif]（Android 系统 Noto Serif），无衬线回退到系统默认
 * （中文环境即 Noto Sans CJK SC）。若要与设计稿逐字形一致，需另行打包字体资产。
 */
@Immutable
data class AppTypography(
    // 衬线：书名、章节、正文
    val bookTitleLarge: TextStyle,
    val chapterTitle: TextStyle,
    val readingBody: TextStyle,
    val coverTitle: TextStyle,
    // 无衬线：界面
    val listTitle: TextStyle,
    val listBody: TextStyle,
    val label: TextStyle,
    val labelStrong: TextStyle,
    val caption: TextStyle,
    val captionStrong: TextStyle,
    val micro: TextStyle,
)

private val Serif = FontFamily.Serif
private val Sans = FontFamily.Default

val AppDefaultTypography = AppTypography(
    // 22px w500 lh1.3
    bookTitleLarge = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Medium,
        fontSize = 22.sp, lineHeight = 28.6.sp,
    ),
    // 20px w500 lh1.5
    chapterTitle = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, lineHeight = 30.sp,
    ),
    /**
     * 正文默认规格 17px w400 lh1.95（设计稿出现 32 次，是正文的基准）。
     * 字号、字重与行距均可由阅读样式抽屉覆盖，此处只是默认值。
     */
    readingBody = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Normal,
        fontSize = 17.sp, lineHeight = 33.2.sp,
    ),
    // 12px w500 lh1.5，封面上的竖排书名
    coverTitle = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 18.sp,
    ),
    // 15px w500 lh1.3
    listTitle = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 19.5.sp,
    ),
    // 15px w400 lh1.3
    listBody = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 19.5.sp,
    ),
    // 13px w500 lh1，界面里最高频的一档
    label = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 13.sp,
    ),
    // 13px w600 lh1
    labelStrong = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 13.sp,
    ),
    // 12px w400 lh1.3
    caption = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 15.6.sp,
    ),
    // 12px w600 lh1，多用于分区小标题与状态角标
    captionStrong = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 12.sp,
    ),
    // 11px w400 lh1
    micro = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 11.sp,
    ),
)

val LocalAppTypography = staticCompositionLocalOf { AppDefaultTypography }

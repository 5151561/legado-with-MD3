package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * kit 的文本原语。
 *
 * 与 `io.legado.app.core.designsystem.component.AppText` 同名不同包，两者刻意分开：
 * 后者是尚未迁移的旧 UI 在用的实现，默认样式取自旧 `LegadoTheme`；本实现只读
 * [AppTheme]，使 kit 与 [io.legado.app.core.designsystem.theme.ProvideAppTheme] 自洽——
 * 不提供旧主题也能渲染。旧 UI 全部迁移后删除 component 包中的那一个。
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val base = style ?: AppTheme.typography.listBody
    val resolved = if (color.isSpecified) color else AppTheme.colorScheme.onSurface
    BasicText(
        text = text,
        modifier = modifier,
        style = base.merge(TextStyle(color = resolved)),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * 富文本重载：同一行里分色的场景（画板 S-04a 的「同时删除本地文件 /Books/…epub」——
 * 路径是次级信息，与前半句同行但不同色）。用两个 [AppText] 拼会在换行时错位。
 */
@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val base = style ?: AppTheme.typography.listBody
    val resolved = if (color.isSpecified) color else AppTheme.colorScheme.onSurface
    BasicText(
        text = text,
        modifier = modifier,
        style = base.merge(TextStyle(color = resolved)),
        maxLines = maxLines,
        overflow = overflow,
    )
}

private val Color.isSpecified
    get() = this != Color.Unspecified

package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.component.AppText
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 错误卡（画板 R-01e「正文加载失败 / 来源失效」）。
 *
 * 圆角 20dp、errorContainer 底、内边距 18dp、子项间距 14dp。
 *
 * 设计规则「状态优先于成功态」要求：阅读器异常每类都要给出「重试 / 换源 / 编辑规则 / 停用」
 * 一类的具体出路，而不是只报一句错。因此 [actions] 不是可选装饰——调用方应当至少给一条出路。
 */
@Composable
fun AppErrorCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val c = AppTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.extraLarge)
            .background(c.errorContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceXxl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceS)) {
            AppText(
                text = title,
                style = AppTheme.typography.listTitle,
                color = c.onErrorContainer,
            )
            if (description != null) {
                AppText(
                    text = description,
                    style = AppTheme.typography.caption,
                    color = c.onErrorContainer,
                )
            }
        }
        actions()
    }
}

package io.legado.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 一块还没重做的界面。
 *
 * 新外壳只装重做过的界面（见 [ShellPlaceholderRoutes]）。这一页把「缺什么、被什么挡着」
 * 直接写在屏幕上，而不是显示一个空白或者假数据——设计规则「状态优先于成功态」在这里
 * 同样适用：没有内容时要给出路，而不是装作一切正常。
 */
@Composable
fun ShellPlaceholderScreen(
    info: PlaceholderInfo,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        AppTopAppBar(title = info.title, titleStyle = AppTopAppBarDefaults.displayTitleStyle)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceGroup),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
            ) {
                AppText(
                    text = "画板 ${info.artboard} 还没重做",
                    style = AppTheme.typography.listTitle.copy(textAlign = TextAlign.Center),
                    color = c.onSurface,
                )
                AppText(
                    text = info.blocker,
                    style = AppTheme.typography.listBody.copy(textAlign = TextAlign.Center),
                    color = c.onSurfaceVariant,
                )
            }
        }
    }
}

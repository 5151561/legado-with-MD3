package io.legado.app.feature.home.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.legado.app.core.designsystem.kit.AppNavigationBar
import io.legado.app.core.designsystem.kit.AppNavigationItem
import io.legado.app.core.designsystem.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * 一级导航栏的对稿装置。
 *
 * 真正的导航栏归 App 外壳所有——首页只通过 `bottomBar` 槽位接收它。这里的实现只服务
 * 预览与截图基线，好让基线图与画板 M-01 / P-01 逐像素对照；外壳接入后不必保留。
 */
internal val MainNavigationPreviewItems = persistentListOf(
    AppNavigationItem(id = "home", label = "首页"),
    AppNavigationItem(id = "shelf", label = "书架"),
    AppNavigationItem(id = "explore", label = "发现"),
    AppNavigationItem(id = "rss", label = "订阅", badgeCount = 12),
    AppNavigationItem(id = "me", label = "我的"),
)

@Composable
internal fun MainNavigationPreviewBar(selectedId: String, modifier: Modifier = Modifier) {
    AppNavigationBar(
        items = MainNavigationPreviewItems,
        selectedId = selectedId,
        onSelect = {},
        modifier = modifier,
    ) { item, selected ->
        val c = AppTheme.colorScheme
        Icon(
            imageVector = item.icon(),
            contentDescription = null,
            tint = if (selected) c.onSecondaryContainer else c.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.dimens.iconLarge),
        )
    }
}

private fun AppNavigationItem.icon(): ImageVector = when (id) {
    "home" -> Icons.Filled.Home
    "shelf" -> Icons.AutoMirrored.Outlined.MenuBook
    "explore" -> Icons.Outlined.Explore
    "rss" -> Icons.Outlined.RssFeed
    else -> Icons.Outlined.Person
}

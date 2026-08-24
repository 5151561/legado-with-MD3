package io.legado.app.ui.shell

import io.legado.app.core.navigation.AppRoute
import io.legado.app.core.navigation.TopLevelRoute
import io.legado.app.feature.settings.ui.ProfileRoute
import kotlinx.serialization.Serializable

/**
 * 外壳的一级导航目的地。
 *
 * 已重做的一级页面把路由声明在自己的 feature 里（如 [ProfileRoute] 在 `settings:ui`）；
 * 尚未重做的一级页面在这里留一个占位路由。**占位集中在这一个文件**——
 * 「还差哪几块」因此是一眼可数的，而不是散落在各 feature 里的半成品。
 *
 * 每重做一块，就把这里的占位删掉，换成那个 feature 自己的 `xxxEntries()`。
 */
@Serializable
data object HomeTabRoute : TopLevelRoute {
    override val id: String get() = "home"
}

@Serializable
data object BookshelfTabRoute : TopLevelRoute {
    override val id: String get() = "bookshelf"
}

@Serializable
data object ExploreTabRoute : TopLevelRoute {
    override val id: String get() = "explore"
}

@Serializable
data object RssTabRoute : TopLevelRoute {
    override val id: String get() = "rss"
}

/** 五个一级目的地，顺序即导航栏顺序（画板 M-01 / P-01 的底栏）。 */
val ShellTopLevelRoutes: List<TopLevelRoute> = listOf(
    HomeTabRoute,
    BookshelfTabRoute,
    ExploreTabRoute,
    RssTabRoute,
    ProfileRoute,
)

/** 尚未重做的一级目的地。列表为空时本文件与 [ShellPlaceholderScreen] 一并删除。 */
val ShellPlaceholderRoutes: Map<TopLevelRoute, PlaceholderInfo> = mapOf(
    HomeTabRoute to PlaceholderInfo(
        title = "首页",
        artboard = "M-01",
        blocker = "home:api 尚未建立，首页的继续阅读、阅读目标与精选都还没有数据来源",
    ),
    BookshelfTabRoute to PlaceholderInfo(
        title = "书架",
        artboard = "B-01",
        blocker = "书架画板尚未导入，bookshelf:api 也还没按旧 UI 行为盘点扩面",
    ),
    ExploreTabRoute to PlaceholderInfo(
        title = "发现",
        artboard = "S-03",
        blocker = "发现分类页尚未重做",
    ),
    RssTabRoute to PlaceholderInfo(
        title = "订阅",
        artboard = "F-01",
        blocker = "订阅画板尚未重做，rss:api 需按旧 UI 行为盘点扩面",
    ),
)

/**
 * 一块还没重做的界面。
 *
 * @param artboard 对应画板号。写出来是为了让占位页自己说清「该长什么样」，
 *   而不是一句笼统的「敬请期待」。
 * @param blocker 挡着它的是什么。这一条是排期依据，不是给用户看的道歉。
 */
data class PlaceholderInfo(
    val title: String,
    val artboard: String,
    val blocker: String,
)

/** 外壳自己的二级目的地。目前只有占位页需要。 */
@Serializable
data class NotRebuiltRoute(val name: String) : AppRoute

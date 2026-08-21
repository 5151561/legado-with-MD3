package io.legado.app.ui.widget.components.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

/**
 * 去除旧玻璃实现后的兼容协议。业务页面只依赖折叠进度与 nested scroll，具体行为继续由
 * Material 3 [TopAppBarScrollBehavior] 提供。
 */
interface GlassTopAppBarScrollBehavior {
    val nestedScrollConnection: NestedScrollConnection
    val collapsedFraction: Float
}

@OptIn(ExperimentalMaterial3Api::class)
class M3GlassScrollBehavior(
    val m3Behavior: TopAppBarScrollBehavior,
) : GlassTopAppBarScrollBehavior {
    override val nestedScrollConnection: NestedScrollConnection
        get() = m3Behavior.nestedScrollConnection

    override val collapsedFraction: Float
        get() = m3Behavior.state.collapsedFraction
}

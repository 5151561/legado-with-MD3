package io.legado.app.ui.widget.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarScrollBehavior
import io.legado.app.ui.widget.components.topbar.M3GlassScrollBehavior

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    topPadding: Dp = 0.dp,
    scrollBehavior: GlassTopAppBarScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val actualEnabled = enabled && (
            isRefreshing ||
            state.distanceFraction > 0f ||
            scrollBehavior == null ||
            if (scrollBehavior is M3GlassScrollBehavior) {
                val appbarState = scrollBehavior.m3Behavior.state
                (appbarState.heightOffsetLimit == 0f || appbarState.heightOffset >= 0f) && appbarState.contentOffset >= 0f
            } else {
                scrollBehavior.collapsedFraction <= 0f
            }
        )
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        enabled = actualEnabled,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .padding(top = topPadding)
                    .align(Alignment.TopCenter),
            )
        }
    ) {
        content()
    }
}

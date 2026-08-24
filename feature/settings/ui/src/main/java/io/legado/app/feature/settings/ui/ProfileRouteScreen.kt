package io.legado.app.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

/**
 * 「我的」（画板 P-01）的路由层。
 *
 * 页面本身不认识任何路由：它只发 [ProfileEntryId]，去处由宿主决定。
 * [ProfileEntryId.WebService] 是唯一不会到达 [onOpenEntry] 的一项——
 * 它在页面上就地开关，不去任何地方。
 *
 * @param bottomBar 一级导航栏由 App 外壳提供。
 */
@Composable
fun ProfileRouteScreen(
    onOpenEntry: (ProfileEntryId) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.OpenEntry -> onOpenEntry(effect.id)
            }
        }
    }

    ProfileScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        bottomBar = bottomBar,
    )
}

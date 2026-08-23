package io.legado.app.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.legado.app.core.designsystem.kit.AppIconSlot
import io.legado.app.core.designsystem.kit.AppOutlinedButton
import io.legado.app.core.designsystem.kit.AppSwitch
import io.legado.app.core.designsystem.kit.AppText
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * 首页区块设置（重设计画板 M-01a v2）。
 *
 * 顶部预览随开关与顺序实时变化——它直接由 [HomeSectionsUiState.visible] 渲染，
 * 没有第二份数据，因此不会出现「预览与实际不一致」。
 *
 * 拖拽排序在这里就地实现：拖动是短暂的手势状态，属于 UI，落位结果才发
 * [HomeSectionsIntent.Move] 交给上层持久化。
 */
@Composable
fun HomeSectionsScreen(
    state: HomeSectionsUiState,
    onIntent: (HomeSectionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        AppTopAppBar(
            title = "首页区块",
            titleStyle = AppTopAppBarDefaults.displayTitleStyle,
            navigationIcon = {
                AppIconSlot(onClick = { onIntent(HomeSectionsIntent.Back) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(dimens.iconLarge),
                    )
                }
            },
            actions = {
                AppOutlinedButton(
                    text = "恢复默认",
                    onClick = { onIntent(HomeSectionsIntent.RestoreDefaults) },
                    contentColor = c.primary,
                    modifier = Modifier.padding(end = dimens.spaceM),
                )
            },
        )

        SectionPreview(
            state = state,
            modifier = Modifier.padding(
                start = dimens.spaceContent,
                end = dimens.spaceContent,
                top = dimens.spaceXs,
                bottom = dimens.spaceXxl,
            ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spaceXs),
        ) {
            ListHeader("显示中 · 拖动排序")
            ReorderableSections(
                sections = state.visible,
                onIntent = onIntent,
            )
            Box(
                Modifier
                    .padding(horizontal = dimens.spaceXl, vertical = dimens.spaceS)
                    .fillMaxWidth()
                    .height(dimens.divider)
                    .background(c.outlineVariant),
            )
            ListHeader("已隐藏 · 数据不丢")
            state.hidden.forEach { section ->
                SectionRow(
                    section = section,
                    visible = false,
                    onIntent = onIntent,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimens.spaceContent,
                    end = dimens.spaceContent,
                    top = dimens.spaceXl,
                    bottom = dimens.spaceGroup +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceL),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = c.outline,
                modifier = Modifier.size(dimens.iconSmall),
            )
            AppText(
                text = "首页可整体隐藏：在设置 · 通用 → 一级导航定制（M-03）",
                style = AppTheme.typography.caption.copy(lineHeight = 18.sp),
                color = c.outline,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ListHeader(text: String) {
    AppText(
        text = text,
        style = AppTheme.typography.label.copy(fontSize = 14.sp),
        color = AppTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = AppTheme.dimens.spaceContent,
            vertical = AppTheme.dimens.spaceL,
        ),
    )
}

/**
 * 顶部实时预览：把「显示中」的区块按当前顺序缩略成条。
 * 「继续阅读」是首页的主卡，预览里也更高、用强调色，与首页观感对应。
 */
@Composable
private fun SectionPreview(state: HomeSectionsUiState, modifier: Modifier = Modifier) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.extraLarge)
            .background(c.surfaceContainer)
            .padding(horizontal = dimens.spaceContent, vertical = dimens.spaceXxl),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceL),
    ) {
        AppText(
            text = "预览",
            style = AppTheme.typography.micro.copy(letterSpacing = 0.88.sp),
            color = c.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
            state.visible.forEach { section ->
                val emphasized = section.id == HomeSectionId.ContinueReading
                val tertiary = section.id == HomeSectionId.BackupReminder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (emphasized) 34.dp else 26.dp)
                        .clip(if (emphasized) RoundedCornerShape(10.dp) else AppTheme.shapes.small)
                        .background(
                            when {
                                emphasized -> c.primary
                                tertiary -> c.tertiaryContainer
                                else -> c.surfaceContainerLowest
                            },
                        )
                        .then(
                            if (emphasized || tertiary) {
                                Modifier
                            } else {
                                Modifier.border(dimens.divider, c.outlineVariant, AppTheme.shapes.small)
                            },
                        )
                        .padding(horizontal = dimens.spaceL),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AppText(
                        text = section.previewLabel(),
                        style = AppTheme.typography.micro,
                        color = when {
                            emphasized -> c.onPrimary
                            tertiary -> c.onTertiaryContainer
                            else -> c.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 预览条里的短标签。放在 UI 层而非契约里——[HomeSectionId] 是业务标识，
 * 缩略只是为了在 26dp 高的条里放得下。
 */
private fun HomeSectionUi.previewLabel(): String = when (id) {
    HomeSectionId.ReadingGoal -> "目标与统计"
    else -> title
}

/**
 * 「显示中」列表，支持长按拖拽换序。
 *
 * 行高不定（说明文字可能折行），因此索引换算用各行实测高度累加，而不是假设等高。
 * 拖拽期间不重排真实列表：位移只由 translationY 表达，落位后才发
 * [HomeSectionsIntent.Move] 交给上层重排，避免手势中途因外部状态刷新而跳位。
 */
@Composable
private fun ReorderableSections(
    sections: ImmutableList<HomeSectionUi>,
    onIntent: (HomeSectionsIntent) -> Unit,
) {
    val heights = remember { mutableStateMapOf<Int, Int>() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    /** 把当前竖直位移换算成落位索引。 */
    fun targetIndex(): Int {
        if (draggingIndex < 0) return -1
        var index = draggingIndex
        var remaining = dragOffset
        while (remaining > 0 && index < sections.lastIndex) {
            val next = heights[index + 1] ?: break
            if (remaining < next / 2f) break
            remaining -= next
            index++
        }
        while (remaining < 0 && index > 0) {
            val prev = heights[index - 1] ?: break
            if (-remaining < prev / 2f) break
            remaining += prev
            index--
        }
        return index
    }

    Column(Modifier.fillMaxWidth()) {
        sections.forEachIndexed { index, section ->
            val dragging = index == draggingIndex
            SectionRow(
                section = section,
                visible = true,
                onIntent = onIntent,
                modifier = Modifier
                    .onSizeChanged { heights[index] = it.height }
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                dragHandle = if (section.locked) {
                    null
                } else {
                    Modifier.pointerInput(section.id, sections) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                            },
                            onDragEnd = {
                                val target = targetIndex()
                                draggingIndex = -1
                                dragOffset = 0f
                                if (target != index) {
                                    onIntent(HomeSectionsIntent.Move(section.id, target))
                                }
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffset = 0f
                            },
                        )
                    }
                },
            )
        }
    }
}

/** 区块行：拖拽把手 + 标题与说明 + 开关，最小高 60dp。 */
@Composable
private fun SectionRow(
    section: HomeSectionUi,
    visible: Boolean,
    onIntent: (HomeSectionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: Modifier? = null,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .defaultMinSize(minHeight = 60.dp)
            .padding(
                start = dimens.spaceXs,
                end = dimens.spaceXl,
                top = dimens.spaceM,
                bottom = dimens.spaceM,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Box(
            modifier = (dragHandle ?: Modifier)
                .width(32.dp)
                .height(dimens.minTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragIndicator,
                contentDescription = if (dragHandle != null) "长按拖动排序" else null,
                tint = c.outlineVariant,
                modifier = Modifier.size(dimens.iconMedium),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXxs),
        ) {
            AppText(
                text = section.title,
                style = AppTheme.typography.listBody.copy(lineHeight = 19.5.sp),
                color = if (visible) c.onSurface else c.onSurfaceVariant,
            )
            AppText(
                text = section.note,
                style = AppTheme.typography.micro.copy(lineHeight = 14.3.sp),
                color = c.outline,
            )
        }
        // 锁定项恒开且点不动：用停用态表达，而不是另造一个第三态。
        AppSwitch(
            checked = visible || section.locked,
            enabled = !section.locked,
            onCheckedChange = { onIntent(HomeSectionsIntent.SetVisible(section.id, it)) },
        ) { tint ->
            Icon(
                imageVector = if (section.locked) Icons.Outlined.Lock else Icons.Outlined.Check,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(name = "M-01a v2 首页区块 · 日光", widthDp = 390, heightDp = 844)
@Composable
private fun HomeSectionsLightPreview() {
    ProvideAppTheme(dark = false) {
        HomeSectionsScreen(state = HomeSectionsPreviewState, onIntent = {})
    }
}

@Preview(name = "M-01a v2 首页区块 · 夜墨", widthDp = 390, heightDp = 844)
@Composable
private fun HomeSectionsDarkPreview() {
    ProvideAppTheme(dark = true) {
        HomeSectionsScreen(state = HomeSectionsPreviewState, onIntent = {})
    }
}

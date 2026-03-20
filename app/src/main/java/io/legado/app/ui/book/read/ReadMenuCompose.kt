package io.legado.app.ui.book.read

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.legado.app.R

enum class MenuMode {
    Main, Progress, Style
}

@Composable
fun ReadMenuCompose(
    modifier: Modifier = Modifier,
    durChapterIndex: Int,
    chapterSize: Int,
    durPageIndex: Int,
    pageSize: Int,
    menuAlpha: Float,
    backgroundColor: Int,
    toolButtons: List<ReadMenu.ToolButton>,
    onEvent: (MenuEvent) -> Unit
) {
    var menuMode by remember { mutableStateOf(MenuMode.Main) }

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        shape = MaterialTheme.shapes.extraLarge,
        color = androidx.compose.ui.graphics.Color(backgroundColor).copy(alpha = menuAlpha),
        tonalElevation = 0.dp
    ) {
        AnimatedContent(
            targetState = menuMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "MenuContent"
        ) { targetMode ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (targetMode) {
                    MenuMode.Main -> MainMenu(
                        toolButtons = toolButtons,
                        onModeChange = { menuMode = it },
                        onEvent = onEvent
                    )
                    MenuMode.Progress -> ProgressMenu(
                        durChapterIndex = durChapterIndex,
                        chapterSize = chapterSize,
                        durPageIndex = durPageIndex,
                        pageSize = pageSize,
                        onBack = { menuMode = MenuMode.Main },
                        onEvent = onEvent
                    )
                    MenuMode.Style -> StyleMenu(
                        onBack = { menuMode = MenuMode.Main }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenu(
    toolButtons: List<ReadMenu.ToolButton>,
    onModeChange: (MenuMode) -> Unit,
    onEvent: (MenuEvent) -> Unit
) {
    // Top Row: Full Text Search entrance
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onEvent(MenuEvent.ToolButtonClick("search")) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.search_content),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        TextButton(onClick = { onModeChange(MenuMode.Progress) }) {
            Text(stringResource(R.string.control_progress))
        }
    }

    // Dynamic Tool Buttons Grid/Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter out 'search' as it's now in the top row
        toolButtons.filter { it.id != "search" }.forEach { btn ->
            MenuIconButton(
                iconRes = btn.iconRes,
                label = btn.description,
                onClick = { onEvent(MenuEvent.ToolButtonClick(btn.id)) },
                onLongClick = { onEvent(MenuEvent.ToolButtonLongClick(btn.id)) }
            )
        }
    }
}

@Composable
fun ProgressMenu(
    durChapterIndex: Int,
    chapterSize: Int,
    durPageIndex: Int,
    pageSize: Int,
    onBack: () -> Unit,
    onEvent: (MenuEvent) -> Unit
) {
    var tempChapterIndex by remember(durChapterIndex) { mutableStateOf(durChapterIndex.toFloat()) }
    var tempPageIndex by remember(durPageIndex) { mutableStateOf(durPageIndex.toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.progress_control),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chapter Slider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onEvent(MenuEvent.PrevChapter) },
                enabled = durChapterIndex > 0
            ) {
                Icon(painterResource(R.drawable.ic_previous), contentDescription = stringResource(R.string.previous_chapter))
            }

            Slider(
                value = tempChapterIndex,
                onValueChange = { tempChapterIndex = it },
                onValueChangeFinished = { onEvent(MenuEvent.SeekToChapter(tempChapterIndex.toInt())) },
                valueRange = 0f..maxOf(0.001f, (chapterSize - 1).toFloat()),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { onEvent(MenuEvent.NextChapter) },
                enabled = durChapterIndex < chapterSize - 1
            ) {
                Icon(painterResource(R.drawable.ic_next), contentDescription = stringResource(R.string.next_chapter))
            }
        }

        Text(
            text = stringResource(R.string.chapter_progress, tempChapterIndex.toInt() + 1, chapterSize),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        // Page Slider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp)) // Align with sliders

            Slider(
                value = tempPageIndex,
                onValueChange = { tempPageIndex = it },
                onValueChangeFinished = { onEvent(MenuEvent.SeekToPage(tempPageIndex.toInt())) },
                valueRange = 0f..maxOf(0.001f, (pageSize - 1).toFloat()),
                enabled = pageSize > 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Text(
            text = stringResource(R.string.page_progress, tempPageIndex.toInt() + 1, pageSize),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StyleMenu(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
        Text(stringResource(R.string.style_developing))
    }
}

@Composable
fun MenuIconButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    badgeCount: Int = 0
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp).width(56.dp)
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge {
                        Text(text = badgeCount.toString())
                    }
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                shape = MaterialTheme.shapes.medium,
                color = androidx.compose.ui.graphics.Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

sealed class MenuEvent {
    object OpenCatalog : MenuEvent()
    object AutoPage : MenuEvent()
    object OpenSettings : MenuEvent()
    object ToggleNightTheme : MenuEvent()
    object PrevChapter : MenuEvent()
    object NextChapter : MenuEvent()
    data class SeekToChapter(val index: Int) : MenuEvent()
    data class SeekToPage(val index: Int) : MenuEvent()
    data class ToolButtonClick(val id: String) : MenuEvent()
    data class ToolButtonLongClick(val id: String) : MenuEvent()
}

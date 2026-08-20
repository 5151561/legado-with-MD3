package io.legado.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.adaptiveHorizontalPadding(): Modifier {
    return this.padding(horizontal = 16.dp)
}

@Composable
fun Modifier.adaptiveHorizontalPaddingTab(): Modifier {
    return this.padding(start = 0.dp, end = 16.dp)
}

@Composable
fun Modifier.adaptiveHorizontalPadding(
    vertical: Dp,
): Modifier {
    return this.padding(horizontal = 16.dp, vertical = vertical)
}

@Composable
fun Modifier.adaptiveVerticalPadding(): Modifier {
    return this.padding(horizontal = 8.dp)
}

@Composable
fun adaptiveHorizonalPadding(): PaddingValues {
    return PaddingValues(horizontal = 16.dp)
}

@Composable
fun adaptiveContentPaddingOnlyVertical(
    top: Dp,
    bottom: Dp
): PaddingValues {
    return PaddingValues(
        top = top,
        bottom = bottom,
        start = 0.dp,
        end = 0.dp
    )
}

@Composable
fun adaptiveContentPadding(
    top: Dp,
    bottom: Dp
): PaddingValues {
    return PaddingValues(
        top = top + 16.dp,
        bottom = bottom,
        start = 16.dp,
        end = 16.dp
    )
}

@Composable
fun adaptiveContentPadding(
    top: Dp,
    bottom: Dp,
    @Suppress("UNUSED_PARAMETER") legacyHorizontal: Dp,
    m3Horizontal: Dp,
): PaddingValues {
    return PaddingValues(
        top = top + 16.dp,
        bottom = bottom,
        start = m3Horizontal,
        end = m3Horizontal
    )
}

@Composable
fun adaptiveContentPadding(
    top: Dp,
    bottom: Dp,
    horizontal: Dp
): PaddingValues {
    return PaddingValues(
        top = top + 16.dp,
        bottom = bottom,
        start = horizontal,
        end = horizontal
    )
}

@Composable
fun adaptiveContentPaddingBookshelf(
    top: Dp,
    bottom: Dp,
    horizontal: Dp
): PaddingValues {
    return PaddingValues(
        top = top + 8.dp,
        bottom = bottom,
        start = 4.dp + horizontal,
        end = 4.dp + horizontal
    )
}

package io.legado.app.ui.theme

import org.junit.Assert.assertSame
import org.junit.Test

class DesignSystemThemeBridgeTest {

    @Test
    fun `legacy theme locals forward to design system locals`() {
        assertSame(
            io.legado.app.core.designsystem.theme.LocalLegadoColorScheme,
            LocalLegadoColorScheme,
        )
        assertSame(
            io.legado.app.core.designsystem.theme.LocalLegadoTypography,
            LocalLegadoTypography,
        )
        assertSame(
            io.legado.app.core.designsystem.theme.LocalLegadoThemeMode,
            LocalLegadoThemeColors,
        )
    }
}

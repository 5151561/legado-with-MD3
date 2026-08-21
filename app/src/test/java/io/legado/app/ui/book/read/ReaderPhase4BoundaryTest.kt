package io.legado.app.ui.book.read

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReaderPhase4BoundaryTest {
    @Test
    fun `reader api does not expose app runtime types`() {
        val source = projectFile(
            "feature/reader/api/src/main/java/io/legado/app/feature/reader/api/ReaderGateway.kt"
        ).readText().withoutComments()
        val forbidden = listOf(
            "io.legado.app.data",
            "io.legado.app.model",
            "TextPage",
            "TextChapter",
            "ReadBook",
            "android.content",
            "android.view",
        ).filter(source::contains)

        assertTrue("reader:api 泄漏了运行时类型：$forbidden", forbidden.isEmpty())
    }

    @Test
    fun `reader ui only consumes the module safe gateway`() {
        val files = projectFile("feature/reader/ui/src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val forbidden = files.flatMap { file ->
            listOf("io.legado.app.model", "io.legado.app.data", "ReadBook", "TextPage", "TextChapter")
                .filter(file.readText()::contains)
                .map { "${file.name}: $it" }
        }

        assertTrue("reader:ui 绕过了 API：$forbidden", forbidden.isEmpty())
    }

    @Test
    fun `compile flag selects exactly one renderer`() {
        val route = projectFile(
            "app/src/main/java/io/legado/app/ui/book/read/ReadBookRouteScreen.kt"
        ).readText().withoutComments()

        assertTrue(
            "旧 ReadView 必须只在 flag 关闭时创建",
            Regex("""if\s*\(\s*!BuildConfig\.USE_COMPOSE_READER_FEATURE\s*\)\s*\{[\s\S]*?ReadBookViewLayer\(""")
                .containsMatchIn(route),
        )
        assertTrue(
            "Compose renderer 必须只在 flag 开启时创建",
            Regex("""if\s*\(\s*BuildConfig\.USE_COMPOSE_READER_FEATURE\s*\)\s*\{[\s\S]*?FeatureReaderRouteScreen\(""")
                .containsMatchIn(route),
        )
    }

    @Test
    fun `app main source has one module safe reader gateway implementation`() {
        val implementations = projectFile("app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { Regex(""":\s*ReaderSessionGateway\b""").containsMatchIn(it.readText()) }
            .map(File::getName)
            .toList()

        assertEquals(listOf("LegacyReaderAdapter.kt"), implementations)
    }

    private fun String.withoutComments() =
        replace(Regex("""/\*[\s\S]*?\*/"""), "").replace(Regex("""//[^\n]*"""), "")

    private fun projectFile(relativePath: String): File {
        var directory: File? = File("").absoluteFile
        while (directory != null) {
            val candidate = File(directory, relativePath)
            if (candidate.exists()) return candidate
            directory = directory.parentFile
        }
        error("找不到项目文件：$relativePath")
    }
}

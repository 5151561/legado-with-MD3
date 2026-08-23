package io.legado.app.help

import android.content.Context
import io.legado.app.BuildConfig
import io.legado.app.constant.BookType
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.book.BookHelp

/**
 * benchmark 变体的书架夹具。
 *
 * 为什么必须由应用自己生成，而不是从外面往数据库里灌：
 * androidx.benchmark 的 CompilationMode 为了把编译状态重置到确定值，在本项目
 * minSdk 覆盖的 API 上是靠 `pm uninstall` + `pm install` 实现的（其字节码里的
 * reinstallPackage），**每次测量前都会清空应用数据**。外部注入的数据活不过一轮，
 * 书架滚动这类依赖数据量的场景就只能测到一个空列表。
 *
 * 只在 `-PbenchmarkFixtures=true` 构建的变体里启用。release 构建下
 * [BuildConfig.BENCHMARK_FIXTURES] 是编译期常量 false，R8 会把整段代码连同
 * 这些字符串一起消除。
 */
object BenchmarkFixtures {

    private const val ORIGIN = "benchmark://fixture-source"
    private const val ORIGIN_NAME = "压测源"
    private const val COUNT = 120

    /** 可读夹具：唯一一本带章节正文的书，供阅读器翻页场景使用。 */
    private const val READER_BOOK_URL = "$ORIGIN/reader"
    private const val READER_CHAPTERS = 20
    private const val READER_PARAGRAPHS_PER_CHAPTER = 60

    /** 固定时间戳，不用 currentTimeMillis：排序结果每次运行都一致，测量才可比。 */
    private const val BASE_TIME = 1_755_900_000_000L

    private val SURNAMES = listOf(
        "青云", "苍穹", "九州", "星河", "龙渊", "幽兰", "北冥", "破晓",
        "长安", "雪原", "赤霄", "墨海", "云梦", "孤城", "天枢",
    )
    private val SUFFIXES = listOf("志", "传", "录", "纪", "歌", "篇", "行", "诀", "谣", "梦")

    /**
     * 补齐夹具书目。调用方需保证不在主线程（Room 未开启 allowMainThreadQueries，
     * 且这里还要写内容文件）。数据库实例由调用方传入：架构护栏禁止在这里新增对全局单例的引用。
     *
     * 用可读夹具的 bookUrl 做存在性探测：装完第一次启动时插入，之后每次启动直接返回。
     */
    fun seedBlocking(database: AppDatabase) {
        if (!BuildConfig.BENCHMARK_FIXTURES) return
        if (database.bookDao.has(READER_BOOK_URL)) return

        // 书源本身不会被使用（canUpdate=false、正文已落盘），插入只是让 origin 能解析，
        // 书架不至于把这些书当成「书源已失效」另作处理。
        database.bookSourceDao.insert(
            BookSource(
                bookSourceUrl = ORIGIN,
                bookSourceName = ORIGIN_NAME,
                enabled = false,
                enabledExplore = false,
            )
        )
        database.bookDao.insert(*Array(COUNT) { index -> fixtureBook(index) })
        seedReaderBook(database)
    }

    /**
     * 造一本真正能打开的书：章节记录进库，正文用 [BookHelp.saveText] 落到内容文件。
     * 只有这样阅读器才不会去连那个不存在的书源，翻页场景才测得到东西。
     *
     * durChapterTime 取最大值，让它排在书架第一位，benchmark 的「点开第一本书」才点得中。
     */
    private fun seedReaderBook(database: AppDatabase) {
        val chapters = List(READER_CHAPTERS) { index ->
            BookChapter(
                url = "$READER_BOOK_URL/chapter/$index",
                title = "第${index + 1}章 压测正文",
                baseUrl = READER_BOOK_URL,
                bookUrl = READER_BOOK_URL,
                index = index,
            )
        }
        val book = Book(
            bookUrl = READER_BOOK_URL,
            tocUrl = "$READER_BOOK_URL/toc",
            origin = ORIGIN,
            originName = ORIGIN_NAME,
            name = "压测长文",
            author = "压测作者",
            type = BookType.text,
            totalChapterNum = chapters.size,
            latestChapterTitle = chapters.last().title,
            latestChapterTime = BASE_TIME + 60_000L,
            durChapterTitle = chapters.first().title,
            durChapterIndex = 0,
            durChapterTime = BASE_TIME + 60_000L,
            order = -1,
            canUpdate = false,
        )
        database.bookDao.insert(book)
        database.bookChapterDao.insert(*chapters.toTypedArray())
        chapters.forEach { chapter ->
            BookHelp.saveText(book, chapter, chapterContent(chapter.index))
        }
    }

    /** 正文内容固定，不含随机量，保证每轮测量的排版工作量一致。 */
    private fun chapterContent(chapterIndex: Int): String = buildString {
        repeat(READER_PARAGRAPHS_PER_CHAPTER) { paragraphIndex ->
            append("第")
            append(chapterIndex + 1)
            append("章 第")
            append(paragraphIndex + 1)
            append("段。")
            append("这是用于帧率基准测试的固定正文，不含随机内容，")
            append("以保证每一轮测量的排版与绘制工作量完全一致。")
            appendLine()
        }
    }

    /**
     * 压掉全新安装后的首启流程。CompilationMode 每轮都会重装应用，这些一次性界面
     * 每轮都会重新出现，盖住书架让 By.res 找不到节点：
     *
     *  - LocalConfig.isFirstOpenApp 为真时 MainActivity 会直接跳去 WelcomeActivity 并 finish；
     *  - LocalConfig.versionCode 与当前版本不一致时会弹一次更新日志对话框。
     *
     * 两个标记都存在名为 "local" 的 SharedPreferences 里，键名与 LocalConfig 保持一致。
     */
    fun suppressFirstRunUi(context: Context) {
        if (!BuildConfig.BENCHMARK_FIXTURES) return
        context.getSharedPreferences("local", Context.MODE_PRIVATE).edit()
            .putBoolean("firstOpen", false)
            .putLong("appVersionCode", BuildConfig.VERSION_CODE.toLong())
            .apply()
    }

    private fun bookUrl(index: Int) = "$ORIGIN/%04d".format(index)

    private fun fixtureBook(index: Int): Book {
        val totalChapters = 200 + (index * 37) % 1800
        val currentChapter = (index * 13) % totalChapters
        return Book(
            bookUrl = bookUrl(index),
            tocUrl = "${bookUrl(index)}/toc",
            origin = ORIGIN,
            originName = ORIGIN_NAME,
            name = "${SURNAMES[index % SURNAMES.size]}${SUFFIXES[index % SUFFIXES.size]}" +
                "%03d".format(index),
            author = "压测作者%02d".format(index % 17),
            // 不设封面：走本地占位图，避免每个 item 触发网络或文件 IO 污染帧率数据。
            coverUrl = null,
            type = BookType.text,
            totalChapterNum = totalChapters,
            latestChapterTitle = "第${totalChapters}章 终章",
            latestChapterTime = BASE_TIME - index * 60_000L,
            durChapterTitle = "第${currentChapter + 1}章 正文",
            durChapterIndex = currentChapter,
            durChapterTime = BASE_TIME - index * 90_000L,
            order = index,
            // 关掉自动更新：书架加载时不要为了刷新去连那个不存在的书源。
            canUpdate = false,
        )
    }
}

package io.legado.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * 帧率基线。[StartupBenchmarks] 只覆盖冷启动，滚动和翻页这两条真正会掉帧的路径此前没有度量。
 *
 * 存在的目的：给「Compose 稳定性改造」提供验收手段。书架是 Compose 网格，
 * 状态类持有的模型一旦被推断为 unstable，任何一次刷新都会全量重组，
 * 表现就是 frameDurationCpuMs 的 P90/P99 抬高——这里能直接读出来。
 * 阅读器是 View 自绘，作为对照组：改稳定性配置不应该影响它。
 *
 * 用 [CompilationMode.Partial] + [BaselineProfileMode.Require]：量的是线上用户从应用市场
 * 装完立刻打开的形态（只编译内嵌 baseline profile），这样数字跨设备、跨时间才可比。
 *
 * 代价是 CompilationMode 为了重置编译状态会 `pm uninstall` + `pm install` 把被测应用
 * **卸载重装**（字节码里就是 reinstallPackage），由此带来两个隐性副作用，都已有对策：
 *   1. 应用数据被清空 —— 书架夹具改由应用自己在启动时补齐，见
 *      io.legado.app.help.BenchmarkFixtures，需以 -PbenchmarkFixtures=true 构建；
 *   2. 部分 OEM（如索尼）的首启权限确认页被重新武装、盖在应用之上，导致 By.res 找不到
 *      任何节点 —— 用 tools/benchmark/cta-watchdog.sh 在跑测期间自动点掉。
 *
 * ```
 * ./gradlew :baselineprofile:connectedAppBenchmarkReleaseAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=io.legado.baselineprofile.FrameTimingBenchmarks
 * ```
 *
 * 运行前提和 [BaselineProfileGenerator] 一致：真机，且书架里至少有一本书。
 * 书架书目最好多于一屏，否则滚动场景量到的是空动作。
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FrameTimingBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** 由 BookshelfScreen 的 Modifier.testTag 提供，经根节点 testTagsAsResourceId 映射为 resource-id。 */
    private val BOOKSHELF_GRID = "bookshelf_list"

    private val packageName: String
        get() = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "io.legado.app"

    /** 书架网格上下滑动。Compose 重组开销的主要观测点。 */
    @Test
    fun bookshelfScroll() = rule.measureRepeated(
        packageName = packageName,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        iterations = 10,
        setupBlock = {
            pressHome()
            startActivityAndWait(mainActivityIntent(packageName))
            // 书架首屏（封面、分组）加载完才开始滑，否则量到的是加载不是滚动。
            device.wait(Until.hasObject(By.res(BOOKSHELF_GRID)), 10_000)
            device.waitForIdle()
            Thread.sleep(2000)
        },
    ) {
        // 不能直接对这个节点 fling：showFastScroll 打开时，BookshelfScreen 的 modifier
        // 落在 VerticalGridFastScroller 包装层上，带 tag 的节点本身 scrollable=false，
        // 真正可滚的 LazyVerticalGrid 是一个 bounds 完全相同的平行语义节点。
        // 所以只拿它的 bounds，再在范围内做坐标滑动——同时也避开了顶部分组栏和底部导航栏。
        val grid = device.findObject(By.res(BOOKSHELF_GRID))
            ?: error("书架网格未找到：确认 App 停在书架页，且书目多于一屏")
        val bounds = grid.visibleBounds
        val x = bounds.centerX()
        val inset = bounds.height() / 6
        val top = bounds.top + inset
        val bottom = bounds.bottom - inset
        repeat(3) {
            device.swipe(x, bottom, x, top, 10)
            device.waitForIdle()
            device.swipe(x, top, x, bottom, 10)
            device.waitForIdle()
        }
    }

    /** 连续翻页。ReadView 自绘 + ChapterProvider 排版的绘制耗时。 */
    @Test
    fun readerPageTurn() = rule.measureRepeated(
        packageName = packageName,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait(mainActivityIntent(packageName))
            openFirstBook(device)
        },
    ) {
        val w = device.displayWidth
        val h = device.displayHeight
        repeat(20) {
            device.swipe((w * 0.85f).toInt(), h / 2, (w * 0.15f).toInt(), h / 2, 8)
        }
        device.waitForIdle()
    }
}

/**
 * 用显式组件启动，不要用无参 [MacrobenchmarkScope.startActivityAndWait]（走 launcher intent）：
 *
 * 1. 应用声明了 Launcher0..LauncherW 一组 activity-alias 做图标切换，launcher intent 解析到
 *    哪一个并不确定，测量对象会漂。
 * 2. 索尼机型的 CTA 权限确认页只在 launcher intent 这条路径上弹出，它是一个盖在应用之上的
 *    窗口，会让 By.res 找不到任何应用内节点，测量结果为空（FrameTimingMetric 直接报
 *    "Observed no renderthread slices in trace"）。
 *
 * 同理不能用 startupMode = StartupMode.WARM：它会在 setupBlock 之前自己用 launcher intent
 * 预热启动一次，CTA 在那一刻就弹出来了，setupBlock 里再换显式 intent 已经晚了。
 * 进程状态由 setupBlock 里的 pressHome + 显式启动自行控制。
 */
private fun mainActivityIntent(packageName: String): Intent =
    Intent().setClassName(packageName, "io.legado.app.ui.main.MainActivity")

/**
 * 从书架进阅读器。定位策略照搬 [BaselineProfileGenerator]：
 * 书目是 Compose 语义节点，uiautomator 里 clickable=false，UiObject2.click() 不可靠，
 * 因此用书特有的进度描述定位节点后取 bounds 中心做原始坐标点击，失败再退回固定坐标。
 */
private fun openFirstBook(device: UiDevice) {
    device.wait(Until.hasObject(By.res("bookshelf_list")), 10_000)
    device.waitForIdle()
    Thread.sleep(2000)

    val w = device.displayWidth
    val h = device.displayHeight

    val bookMatcher = By.desc(Pattern.compile(".*(未读|已读|读到|第.{1,8}章).*"))
    val bookNode = device.wait(Until.findObject(bookMatcher), 5000)
    if (bookNode != null) {
        val b = bookNode.visibleBounds
        device.click(b.centerX(), b.centerY())
    } else {
        device.click((w * 0.18f).toInt(), (h * 0.37f).toInt())
    }
    device.waitForIdle()
    Thread.sleep(3000)

    // 点书可能进的是书籍详情页，此时再点“阅读”FAB；已直接进阅读器则为 null。
    device.wait(Until.findObject(By.text("阅读")), 2000)?.click()
    device.waitForIdle()

    // 等首屏排版完成，避免把排版耗时算进翻页帧。
    Thread.sleep(3500)
}

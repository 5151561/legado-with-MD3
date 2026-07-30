# 审计：手搓 Compose 组件 vs 官方 M3 组件库

审计范围：项目里有多少地方用了 Compose 但不走官方组件库、自己手搓 —— 且限于**官方本来就能实现**的那种。
只看 Material 3，Miuix 引擎分支不在范围内。

## 版本基线

`gradle/libs.versions.toml:56,10`：`material3 = 1.5.0-alpha23`、Compose BOM `2026.06.01`、
`foundation/animation = 1.11.4`。Material 3 Expressive 的完整 API 面已可用，且多数已在 alpha18–alpha23 转正：

- alpha17 `Slider` 实验 API 转正；alpha18 `WavyProgressIndicator` + `expressiveLightColorScheme` 转正
- alpha19 Expressive Button / Menu / `ToggleButton` / Expressive `ListItem` 转正
- alpha20 `SplitButton` + `FloatingToolbar` 转正；alpha21 `ButtonGroup` 转正
- alpha22 `MediumFlexibleTopAppBar` 等变体 + `SearchBarState` / slot-based `SearchBar` 转正
- alpha23 Expressive `TimePicker`

项目里 **52 个文件已 `@OptIn(ExperimentalMaterial3ExpressiveApi)`** —— 并非不知道这套 API 存在。

## 判定口径

- 项目**有意**维护一层 wrapper（`AppScaffold`/`AppAlertDialog`/`AppIconButton`…），
  `.claude/skills/legado-compose-migration/references/project-patterns.md:143-153` 明确要求优先用项目组件。
  **薄 wrapper 是设计，不计入问题。** 已核实约 40 个文件属于这类正确 wrapper。
- 只统计**重新实现了 M3 已有组件**的情况。
- 排除：Miuix 分支、`ui/widget/` 下约 11,300 行非 Compose 的 View/XML 遗留层、
  以及为真正自定义绘制而用的 `Canvas`（封面、图表、热力图、阅读器翻页、shader 特效）。

## 结论

**约 30 处、约 3,700–4,200 行**手搓了 M3 已有能力。

| 分类 | 规模 | 是否有双引擎理由 |
|---|---|---|
| 共享层纯手搓 | ~2,300 行 / 12 文件 | **无** Miuix 分支 |
| 共享层「有 Miuix 分支但 M3 那半边手搓」 | ~650 行 / 4 文件 | 部分 |
| 业务屏幕内手搓 | ~700 行 / 13 处 | — |

反向指标：`SegmentedButton` / `SingleChoiceSegmentedButtonRow` 全项目 **0 次使用**，
`MaterialShapes` **0 次**，wavy 进度条 **0 次** —— 恰好这几个正是被手搓替代掉的能力。

规模参照：`ui/widget/components/` 141 个 Compose 文件、约 20,000 行；
其中 58 个文件在 `ThemeResolver.isMiuixEngine` 上分支 —— 双引擎抽象解释了一部分（但不是全部）设计。

---

## A. 高杠杆项

这三项都是高扇入，但**改内部实现即可，调用点零改动**。

### A1. `button/series/` 家族 —— 1,021 行手搓按钮系统

13 个文件。核心 `SeriesButton`（`ui/widget/components/button/series/SeriesIconButton.kt:60-120`）=
`Box` + `clip` + `background` + `border` + `combinedClickable(indication = ripple())`
+ 手写 `animateColorAsState(tween(150))` + 手写 `disabledContentColor = copy(alpha = 0.38f)`
+ 手写 `Modifier.semantics { selected = true }`。

对应 M3：`ButtonDefaults.{Small,Medium}ContainerHeight` / `.{…}ContentPadding` / `.{…}IconSize` /
`.{…}Shape`、`IconButtonDefaults.{small,medium}IconSize/*Shape/*ContainerSize`、
`ToggleButton` + `ToggleButtonDefaults`。`tween(150)` 应由 `MotionScheme.expressive()` 提供。

值得记录的点：

- 该文件**已经 import** `IconButtonDefaults.mediumIconSize`、`IconButtonDefaults.extraSmallRoundShape`、
  `minimumInteractiveComponentSize`、`ripple` —— 在挑用 M3 的 token，却拒绝消费这些 token 的 M3 组件。
- 家族内部自相矛盾：`MediumToggleButton.kt` 的 M3 分支**正确使用** `ToggleButton`，
  而 `SmallToggleButton.kt` 走 `SeriesButton` 手搓同一个东西。
- **13 个文件中仅 `MediumToggleButton` 有 Miuix 分支** → 约 880 行没有任何双引擎理由。
- M3 唯一没有的是按钮上的 `onLongClick`，一个 `Modifier.combinedClickable` 的事，不值一套按钮系统。

调用面：`MediumTonalButton` 44 文件、`SmallPlainButton` 29、`SmallTonalButton` 20、`MediumPlainButton` 8、
`SmallToggleButton` 5，`SmallOutlinedButton`/`MediumOutlinedButton`/`MediumToggleButton` 各 3。

### A2. `settingItem/TinySettingItems.kt` —— 775 行手搓列表行

9 个公开 composable。`TinySettingItem` = `NormalCard` → `Row(height = 56.dp, padding 12.dp)`
→ `Icon(18.dp)` → `Column(title/description)` + 尾部 slot + 手写 `alpha = if (enabled) 1f else 0.5f`。
整个 775 行文件只从 M3 import 了 `Icon`。

对应 M3：`ListItem` + `ListItemDefaults.colors()`（Expressive `ListItem` 在 alpha19 已转正）。
**同目录 `settingItem/SettingItem.kt`（194 行）已经用 M3 `ListItem` 正确实现了同一件事。**

当前有三套并行设置行栈：`SettingItem`（M3 `ListItem`）、`TinySettingItem`（手搓）、
`CompactSettingItems.kt`（311 行，委托 `SettingItem`）。统一到 `ListItem` + 一个 density 参数
可去掉 **~700–900 行**。无 Miuix 分支。

调用面：`TinySwitchSettingItem` 14、`TinySliderSettingItem` 10、`TinyClickableSettingItem` 9、
`TinyDropdownSettingItem` 7、`TinySettingItem` 5。

### A3. `card/GlassCard.kt` —— 扇入最高的手搓卡片

247 行；M3 分支约 150 行是 `Surface` + 手写 `combinedClickable` +
手工拆解 `CardDefaults.cardColors(...).containerColor/.contentColor`（`card/GlassCard.kt:52-140`）。
真正的增量（按主题覆盖圆角/描边、九宫格 item 背景、`Color.Transparent` 分支、Miuix 分支）
`Card(shape =, border =, colors =)` 全部直接支持。

**扇入 51 个文件** —— `SettingCard`、`TextCard`、`CheckboxItem`、`CollapsibleHeader`、
`SelectionItemCard`、`LoadMoreFooter`、`CardTabRow`、`TinySettingItem` 都经由它。
换成真 `Card` 可一次性把 elevation、state layer、`LocalContentColor` 传播修正到十几个组件
（但收益受 D1 的 `AppText`/`AppIcon` 限制，见下）。

---

## B. 共享层其余各处

| 位置 | 行数 | 对应 M3 API | 备注 |
|---|---|---|---|
| `SearchBar.kt` | 156 | `SearchBar` + `SearchBarDefaults.InputField` | 见下方专项 |
| `menuItem/RoundDropdownMenuItem.kt` | 197（M3 分支 ~70） | `DropdownMenuItem` + `MenuDefaults.itemColors()` | 手写 enabled×selected 颜色矩阵，`0.38f` 重复 6 次；已 import `MenuDefaults.DropdownMenuItemContentPadding`。**48 个调用文件** |
| `tabRow/CardTabRow.kt` | 105 | `SingleChoiceSegmentedButtonRow` + `SegmentedButton` | 加权 `NormalCard` 行 + `animateColorAsState`。**无 Miuix 分支**；同目录 `AppTabRow.kt` 已正确用 `PrimaryTabRow`。8 调用点 |
| `card/TextCard.kt` | 88 | `AssistChip` / `SuggestionChip`（不可点时 `Badge`） | **37 调用点**；项目其他地方已在用 `AssistChip`（`dialog/CustomTipDialog.kt`、`dialog/TextListInputDialog.kt`、`importComponents/ImportComponents.kt`）—— 自相矛盾 |
| `checkBox/CheckboxItem.kt` | 79 | `ListItem(leadingContent = { Checkbox })` + `Modifier.toggleable` | 手写 `clearAndSetSemantics` 是在消除 `ListItem` 原生 `mergeDescendants` 本可避免的重复播报。11 调用点 |
| `dialog/TimePickerDialog.kt` | 69 | `androidx.compose.material3.TimePickerDialog` | 1.5 已有一等公民版本（含 `TimePickerDialogDefaults` 与 `DisplayModeToggle`，当前实现丢失了模式切换） |
| `divider/SettingItemDivider.kt` | 46 | `HorizontalDivider(modifier = fillMaxWidth(f).clip(CircleShape))` | 无 Miuix 分支 |
| `divider/PillDivider.kt` | 57 | 同上 | 有 Miuix 分支 |
| `divider/PillHeaderDivider.kt` | 57 | `HorizontalDivider` + label | 带标签的形态 M3 无对应，部分合理 |
| `explore/ExploreKindTextField.kt` | 74 | 现成的 `AppDenseTextField`（`AppTextField.kt`，带 `contentPadding`） | 34.dp 高度低于 M3 56.dp 下限，部分自定义合理；但这是项目第三条 text field 路径 |
| `SectionTitle.kt` | 23 | `Text` | 极小 |

### 专项：两个 SearchBar，在用的是错的那个

- `ui/widget/components/AppSearchBar.kt`（168 行）**正确**使用 `SearchBar` / `rememberSearchBarState` /
  `SearchBarValue` / `ExpandedFullScreenSearchBar` / `ExpandedFullScreenContainedSearchBar`
  —— 但**全项目 0 个调用点，是死代码**（`AppSearchBar(` 仅在其自身定义处出现）。
- `ui/widget/components/SearchBar.kt`（156 行）手搓 `Surface(RoundedCornerShape(32.dp))` + `AppDenseTextField`
  + 手工 `FocusRequester`/`SoftwareKeyboardController` 编排 + 手写 `query ↔ TextFieldState` 双向同步
  —— **13 个文件在用**：`SearchScreen.kt`、`BookshelfScreen.kt`、`BookSourceScreen.kt`、
  `AllBookmarkScreen.kt`、`SearchContentScreen.kt`、`RssSortScreen.kt`、`ThemeManageScreen.kt`、
  `ScopeSelectSheet.kt`、`TxtTocRulePreviewScreen.kt`、`CloudTtsScreen.kt`、`ReadRecordScreen.kt`、
  `DynamicTopAppBar.kt`、`ExploreKindSelectSheet.kt`。
- 且它的名字**遮蔽** `androidx.compose.material3.SearchBar` —— import 哪一个是活的陷阱。

---

## C. 业务屏幕内手搓

先说好消息：绝大多数业务屏幕是干净的。sheet 走 `AppModalBottomSheet`、dialog 走 `AppAlertDialog`、
swipe 走真 `SwipeToDismissBox`（`swipe/SwipeActionContainer.kt:74`）、
reorder 走 `sh.calvin.reorderable`（M3 无对应 API，选择正确）、pull-to-refresh 走共享 wrapper。
**没有手搓下拉刷新、没有手搓 bottom sheet、没有 `rotate`+`drawArc` 的手搓转圈、没有手搓 tooltip。**
手搓集中在下列少数几处。

### C1. `ui/book/read/ReadBookMenuBar.kt`（3,472 行，是 Compose，非 View 阅读器表面）

- `:1291-1371` `ReadMenuGlassButtonSurface` —— 双层 `Box` + `clip`/`background`/`border`
  + `combinedClickable` + 手写 `semantics`。三个 `iconStyle` 分支（`:1309-1318`）1:1 对应
  `IconButton` / `FilledTonalIconButton` / `OutlinedIconButton`；`selected` 态对应
  `FilledTonalIconToggleButton` + `IconButtonDefaults.toggleableColors()` / `toggleableShapes()`
  （1.5 带按压形变）。`:1319-1320` 的 48/40dp 内外套娃即 `IconButtonDefaults.mediumContainerSize()`。
  扇入：`:1229` `MenuTitleGlassButton`、`:1255` `ReadMenuGlassIconButton`、`:1957` `FloatingIconRow`、
  `:2538` `BottomBarGlassIconButton`、`:2847` `ToolButtonItem`。
  仅 `readMenuLiquidGlass` 分支（`:1332-1342`）需保留自定义。
- `:1479-1725` `MenuTitleBarMergedGlassButton` → `ButtonGroup` + `VerticalDivider`。
  最多 6 个 40.dp `Box` + `clickable`（`:1499,1533,1567,1601,1635,1669,1703`），
  中间是手绘 1.dp 分隔条 `Box(width(1.dp).height(20.dp).background(tint.copy(alpha = 0.15f)))`
  （`:1523,1557,1591,1624,1658,1692`）。
  ⚠ 这些按钮全部 `indication = null`，**完全没有按压反馈** —— 属体验缺陷，不只是啰嗦。
- `:2607-2813` `ReadMenuLiquidSlider`（~200 行）→ `Slider(track =, thumb =)`。
  自建 drag（`:2661-2691`）、tap-to-seek（`:2718-2729`）、`layout{}` 量测 hack（`:2738-2744`）、
  `graphicsLayer` 拇指定位（`:2750-2756`）、手写 `progressBarRangeInfo`/`setProgress` 语义（`:2631-2652`）。
  `drawBackdrop`/lens/`InnerShadow` 视觉值得保留，但应放进 `Slider` 的 slot，而非另起一套手势+语义栈。
  同文件 `:2593` 非 glass 路径**已正确委托** `AppSlider` —— 单侧漂移。
- `:2863-2893` `ToolButtonItem` 激活态（容器色 `:2831-2835` + `BorderStroke` `:2836-2840` 按 `isActive` 切换）
  → `ToggleButton` / `FilledTonalIconToggleButton`。
- `:2944-2957` 手搓角标（`Text` + `background(error, RoundedCornerShape(8.dp))` 对齐 `TopEnd`）
  → `BadgedBox` + `Badge`；当前丢失 M3 的角标尺寸/偏移/最大计数行为。
- 次要：`:961-973` `drawBehind { drawLine }` 画 1px 底边（颜色/宽度用户可配，尚可）、
  `:2267-2281` `SearchInfoPill` → `Surface(shape = shapes.medium)` 或不可点 `AssistChip`、
  `:2284-2318` `SearchMenuActionButton` → `FilledTonalButton` / `AssistChip`。

### C2. 缺陷 A —— 自定义 Slider 对无障碍完全不可见

`ui/book/readaloud/player/ReadAloudPlayerScreen.kt:406-498` `PlayerProgressSlider`：
`Canvas` + `pointerInput { detectHorizontalDragGestures }`（`:437-455`）+ 手工 `previewFraction` 状态
+ `graphicsLayer { scaleY = trackScale }` 按压缩放（`:424-435`）+ `while` 循环 `drawLine` 画刻度（`:476-486`）。

**通篇没有任何 `progressBarRangeInfo` / `setProgress` 语义** → TalkBack 读不到，也无法用 `setProgress` 操作。

`Slider(steps = N)` 原生画刻度；1.5 的 expressive `Slider` 本身就带这段代码在手动模拟的
「按压加粗轨道 + 收窄拇指」。刻度外观若必须保留，塞进 `Slider(track = { … })` 即可白拿手势与无障碍。

### C3. 缺陷 B —— 首页每帧上百次渐变绘制

`ui/main/home/HomeScreen.kt:923-984` `RecentReadingProgress`：
真正的指示条只是 `:973-982` 两个 `drawRect` → `LinearProgressIndicator(progress = …)`
（或 `LinearWavyProgressIndicator` 拿 expressive 外观）。

问题在 `:944-972` 的光晕循环：`stripHeight` 为 1px、`glowHeight = 48dp - 3dp = 45dp`，
density 3 下**每帧约 135 次 `Brush.horizontalGradient` + `drawRect`**，
且在 `animateFloatAsState` 跑动期间对首页每一行「最近阅读」各跑一遍。
`drawBehind` 里单个 `Brush.verticalGradient` 可替掉整个循环。

### C4. 其余零散处

| 位置 | 对应 M3 API |
|---|---|
| `ui/book/source/manage/BookSourceScreen.kt:477-507` `GroupFilterItem` | `FilterChip` |
| `ui/book/read/sheet/HighlightRuleEditSheet.kt:484-530` | `FilterChip` in `FlowRow` / `MultiChoiceSegmentedButtonRow` |
| `ui/ai/chat/AiChatScreen.kt:883-930`（Off/Auto/Low/Med/High/Max 六档单选，含手写 `Check` 图标 `:918-925`） | `SingleChoiceSegmentedButtonRow` + `SegmentedButton`；若须保持纵向则 `ListItem` + `RadioButton` |
| `ui/main/homepage/HomepageModuleFeed.kt:557-584` `ModuleHeaderTab` + 宿主 `LazyRow` `:514-537` | 现成共享 `AppTabRow`（另有 10 个屏幕已在用，此处是漂移；当前无指示器、无 `TabRow` 语义容器） |
| `HomepageModuleFeed.kt:236-266`、`ui/main/homepage/modules/RankingModule.kt:90-116` | `TextButton`（现状：全宽矩形 ripple + 无 48dp 触达） |
| `ui/replace/ReplaceRuleScreen.kt:230` 裸 `Dialog { LoadingIndicator() }` | `BasicAlertDialog`（当前无 M3 surface/shape/scrim token） |
| `ui/book/read/sheet/ClickActionConfigSheet.kt:68-72` 手搓全屏遮罩 + `:40` 手写 `BackHandler`；`:189` 可点行 | `BasicAlertDialog(DialogProperties(usePlatformDefaultWidth = false))` 可一并删掉 `BackHandler`；选项行 → `ListItem` + `RadioButton` |

---

## D. 结构性决策（不是清理，需要拍板）

### D1. `text/AppText.kt`(122) + `icon/AppIcon.kt`(87) 重实现 `Text` / `Icon`

`AppText` 包 `BasicText` 并重实现 M3 `Text` 的 style-merge 与颜色回退级联；
`AppIcon` 包 `Modifier.paint` 并重实现 `Icon` 的 `defaultSizeFor`（24.dp 回退）、
tint→`ColorFilter`、`Role.Image` 语义。

文件内注释写明理由：*「这完美避开了 M3 的 LocalTextStyle，且自动适配 Miuix/M3 引擎」*
—— 刻意绕开 `LocalTextStyle`/`LocalContentColor`，让 `LegadoTheme.typography/colorScheme` 同时服务两个引擎。
**理由真实**，是 `theme/LegadoTheme.kt` 平行 token 系统（`LegadoColorScheme`/`LegadoTypography`/
`LocalLegadoColorScheme`，即 `MaterialTheme.*` 的影子）的必然结果。

代价：所有提供 `LocalContentColor` 的 M3 组件（`ListItem`、`Card`、`Surface`、`Button`、`TopAppBar`）
对其内部文字/图标颜色**静默失效** —— `SeriesButtonContent` 里颜色必须以 lambda 参数手工穿透，
就是这个代价的显影。这也是 A3 换成真 `Card` 后收益受限的根因。

**建议：保留，但把该 trade-off 明确记录下来**，以免后来者反复尝试
「为什么 `Card` 里的文字颜色不跟着变」。

### D2. 两个底部栏并存

`FloatingBottomBar.kt`（447 行；liquid-glass + damped-drag 指示器 + `com.kyant.backdrop` blur/lens/vibrancy；
源自 KernelSU，GPL-3.0 头）与 `navigation/AppNavigationBar.kt`（176 行；真 `ShortNavigationBar`）并存，
由 `appShell.useFloatingBottomBar` 切换。前者交互模型 M3 确实没有，属合理自定义，
但两套并存是长期维护成本，需明确是否长期保留。

---

## E. 顺带发现的独立缺陷（与 M3 迁移无关）

1. `lazylist/LazyList.kt` `ScrollbarLazyColumn` 计算了 `positionOffset` 却**从未使用**
   —— 实际是 `LazyColumn` 的空壳包装。死抽象，应实现或删除。
2. `text/AnimatedText.kt` `AnimatedText` **对每个字符各起一个 `AnimatedContent` 排在 `Row` 里**，
   破坏文字整形、连字、文本选择与 RTL。用于数字/计数器可以，
   但它经 `AdaptiveAnimatedText` 被喂给了 app bar 标题。
3. `alert/AppAlertDialog.kt:39,41,154,156` 默认按钮文案硬编码中文 `"确定"`/`"取消"`，未走 string resource
   （`button/ToggleChip.kt` 的 `"已选择"`/`"未选择"` 同）。
4. `LoadMoreFooter.kt`（324 行）同一个「info card + `HorizontalDivider` + action row」块近乎逐字重复 3 次，
   约 150 行是复制粘贴。
5. 三个进度 wrapper（`progressIndicator/App{Circular,Linear,ContainedLoading}*`）都没用上 wavy 变体
   —— `LinearWavyProgressIndicator`/`CircularWavyProgressIndicator` 是躺在门口的 expressive 升级，
   且改 wrapper 内部即可，调用点零改动。
6. `components/AccentColorButton.kt` 与遗留 View 版 `ui/widget/AccentColorButton.kt` 同名冲突。

---

## 明确核查过、判定为合理的部分（不是问题）

- **真正自定义绘制**：`HighlightRuleEditSheet.kt:661-760`（`textMeasurer` + 下划线样式预览）、
  `:815-860`（九宫格裁剪辅助线）、`ThemeConfigScreen.kt:1213-1231`（两个 `drawArc` 组成主题色板，
  非进度环）、`ReadRecordScreen.kt:1153`、`readRecord/component/ReadRecordCharts.kt`、
  `ReadRecordHeatmap.kt`（图表/热力图）、`book/read/page/**`、`manga/entities/*Transformation.kt`。
- `HomeScreen.kt:1109-1157` `SemiCircleProgress`：180° 仪表盘。M3 无部分扫角的圆形指示器，绘制合理
  （仅注意它手写了 `progressBarRangeInfo`）。
- **无限动画**：`SkeletonPlaceholders.kt:40-50`（shimmer，M3 无 skeleton API）、
  `TranslationThinkingCapsule.kt:27-36`（M3 `Surface` 上的呼吸 alpha）、
  `AiGeneratedMessageContent.kt:348-366`（`StreamingDots`）—— 均非手搓 spinner。
- `book/read/ReadAloudCapsule.kt:142-160`：可拖动悬浮胶囊，M3 无对应，保留。
- `book/read/TextActionSelectionMenu.kt:122,222`：`Popup` 锚定的文本选择工具条（覆盖在 `AndroidView` 之上）。
  M3 无浮动选择工具栏，保留 —— 且它**不是**伪装的 bottom sheet。
- `ui/main/MainScreen.kt:308-321,405-415,606`：在 `WideNavigationRailItem` 上叠
  `combinedClickable(indication = null)` 纯粹为补 `onLongClick`（书架分组菜单），M3 导航项未暴露该回调。
  可接受的 workaround。
- **硬编码字面量不算严重**：`Color(0x…)` 命中全在 `ui/theme/colorScheme/**` 主题定义里；
  `ReadAloudPlayerScreen.kt:869-877` 是刻意的 blend-mode 常量。
  硬编码 `RoundedCornerShape(N.dp)` 峰值为 `ReadBookMenuBar.kt` 11 处、`main/bookshelf/BookItem.kt` 7 处
  —— 值得日后归一到 `MaterialTheme.shapes`，但非优先项。
- `swipe/SwipeActionContainer.kt`、`AppPullToRefresh.kt`、`AppSlider.kt`、`AppTabRow.kt`、
  `AppNavigationBar.kt`、`settingItem/SettingItem.kt`、`AppFloatingActionButton.kt`、
  `SelectionBottomBar.kt`、`topbar/*`、`alert/AppAlertDialog.kt`、`modalBottomSheet/*` 等
  约 40 个文件是**正确的薄 wrapper**，无需处理。

---

## F. 换成官方组件后外观会变吗？逐项数值对照

结论先行：**没有一个是严格 1:1 的**——每个手搓组件都编码了刻意或非刻意偏离 M3 默认值的地方，
直接换成官方组件、全用默认参数，外观一定会变。但绝大多数偏离都能通过 M3 组件暴露的参数
（`colors`/`shape`/`contentPadding`/`border`/`Modifier.size`）显式传回来做到零视觉变化，
真正无法还原、必须做设计取舍的只有下面 5 处。

以下 M3 侧数值取自 `androidx-main` 的 token 源码（`ButtonSmallTokens`/`ButtonXSmallTokens`/
`ListTokens`/`FilledCardTokens`/`AssistChipTokens`/`ShapeTokens`），**非 alpha23 精确 tag**，
个别数值可能有细微出入，动手前建议用项目实际依赖的 aar 复核一遍。

### F1. `Small*`/`Medium*Button` 系列——意外地接近 M3，但名字对错了档位

| | 项目 `MediumTonalButton` | M3 `ButtonSmallTokens` | 是否吻合 |
|---|---|---|---|
| 容器高度 | 40.dp | **40.dp** | ✓ |
| 水平内容内距 | 16.dp | **16.dp**（`LeadingSpace`/`TrailingSpace`） | ✓ |
| 图标-文字间距 | 8.dp | **8.dp**（`IconLabelSpace`） | ✓ |
| 形状 | `extraSmallRoundShape`（圆） | `ContainerShapeRound = CornerFull` | ✓ |
| Outlined 描边宽度 | 1.dp | **1.dp**（`OutlinedOutlineWidth`） | ✓ |
| 图标尺寸 | 24.dp | 20.dp（`IconSize`） | ✗ |

`Small*Button`（32×32、圆形、水平内距 8dp）同样精确对上 M3 **ExtraSmall**（`ButtonXSmallTokens.ContainerHeight = 32.dp`，`LeadingSpace`/`TrailingSpace = 16.dp` 与项目 8dp 内距不同但同量级，形状同为 `CornerFull`）。

**这里有一个真陷阱**：项目命名的 Small/Medium 数值上实际对应 M3 的 **ExtraSmall/Small**。
若按名字直译迁移（项目 Medium → M3 `Button()` 默认档），容器高度会从 40dp 直接跳到 56dp，
整体按钮明显变大；必须按**数值**对应（项目 Small→M3 ExtraSmall，项目 Medium→M3 Small）才能保持零变化。

其余偏离（容器色 `surfaceContainerLow` vs `secondaryContainer`、内容色 `onSurfaceVariant` vs
`onSecondaryContainer`、禁用态容器色 `outlineVariant` 不透明 vs `onSurface × 0.12f` 半透明）
全部可以通过 `ButtonDefaults.filledTonalButtonColors(containerColor=, contentColor=,
disabledContainerColor=, disabledContentColor=)` 显式传回来还原，图标尺寸也只是一个
`Modifier.size(24.dp)` 的事——都不是 M3 强制的。

### F2. `GlassCard`/`NormalCard`——elevation 本来就一致，颜色可传参还原

| | 项目 | M3 `FilledCardTokens` | 是否吻合 |
|---|---|---|---|
| elevation | 0.dp | `ContainerElevation = Level0` = **0.dp** | ✓（本来就一致） |
| 圆角 | 16.dp（继承 Miuix 默认）；94 处调用点显式传值（12dp×40、8dp×19、4dp×19、16dp×9…） | `CornerMedium` = 12.dp | 大部分调用点已显式传值，可控 |
| 容器色 | `surfaceContainer` | `SurfaceContainerHighest` | 可通过 `CardDefaults.cardColors(containerColor=)` 还原 |
| 内容色（`GlassCard` 分支） | **`onSecondaryContainer`**（可疑默认值，非 `contentColorFor(containerColor)`） | `contentColorFor(container)` = 通常是 `onSurface` | 可传参还原，但当前默认值本身值得单独核查是不是笔误 |
| 按压/悬停 elevation | 无（`Surface` + `combinedClickable`，elevation 恒定） | filled card hover = `Level1` | 换成 `Card(onClick=)` 会**引入**这个抬升——是 M3 会新增的行为，不是丢失 |

### F3. `TinySettingItem` → `ListItem`——唯一「传参也救不回来」的尺寸变化

项目固定 `Modifier.height(56.dp)`，不管有没有 description 都是 56dp。
M3 `ListItem` 按 one/two/three-line **强制最小高度**（`ItemOneLineContainerHeight = 56.dp`、
`ItemTwoLineContainerHeight = 72.dp`、`ItemThreeLineContainerHeight = 88.dp`），这是布局层面
强制的，不是默认参数，**传不进去**。

影响面：单行设置项（无 description）56dp 恰好吻合，**零变化**；带 description 的两行设置项
会从 56dp 强制变成 72dp——这是全审计范围内唯一一处「即使显式传参也无法保持原尺寸」的情况。

alpha23 的新 `ListItem` overload 好消息更多：暴露了 `contentPadding`（可传 12dp 保持内距）、
**不强制** leading icon 尺寸（可保 18dp，不会被拉到 M3 默认的 24dp/`ItemLeadingIconSize`）、
有 `shapes`/`colors` 参数（可保 12dp 圆角与 `surfaceContainerLow` 容器色）。所以除了两行行高，
其余视觉几乎都能保留。

### F4. `CardTabRow` → `SegmentedButton`——不是换皮，是重新设计

现状是 8dp 间隙分离的独立卡片（`Arrangement.spacedBy(8.dp)`）；M3 `SegmentedButton` 是
**连体、零间隙、带描边、选中态默认带 `Check` 图标**的分段控件。选中色 `secondaryContainer`
正好和项目现值吻合，但整体几何、描边、勾选图标都是当前设计里没有的元素，属于外观上的
主动改版，不是「换个实现细节」。

### F5. `TextCard` → `AssistChip`——尺寸被强制拉大

| | 项目 `TextCard` | M3 `AssistChipTokens` | 是否吻合 |
|---|---|---|---|
| 圆角 | 8.dp | `CornerSmall` = **8.dp** | ✓ |
| 内容色 | `onSurface` | `LabelTextColor = OnSurface` | ✓ |
| 容器高度 | ~24dp（无固定高度，由文字+2×4dp内距撑开） | `ContainerHeight = 32.0.dp`（**强制**） | ✗ 会变高 |
| 标签字号 | `labelSmallEmphasized`（11sp/Medium） | `LabelLarge`（14sp） | ✗ 会变大 |
| 图标尺寸 | 14.dp | `IconSize = 18.0.dp` | ✗ 会变大 |
| 描边 | 无 | 默认 flat 态带 `1.dp OutlineVariant` 描边 | ✗ 新增描边（可传 `border = null` 去掉） |

`TextCard` 目前用作紧凑的「当前值」标签（如设置项尾部的取值展示），`AssistChip` 的 32dp
强制高度和 14sp 字号明显偏大，不适合这个场景；换成 `AssistChip` 前需要先确认视觉预算能接受，
或考虑用 `Badge` 承载不可点场景。

### F6. `TimePickerDialog` → M3 `TimePickerDialog`

对话框容器规格换成 M3 标准（内距、圆角、按钮布局跟随 M3 dialog token），且会**多出一个
时钟/输入模式切换按钮**（`DisplayModeToggle`）——这是当前手搓实现缺失、换过去后新增的功能，
而非丢失，但仍属于外观变化，需要在切换前对齐产品预期。

### 小结

| 组件 | 换成 M3 后是否可做到零视觉变化 |
|---|---|
| `Small*`/`Medium*Button` 系列 | **可以**（六项参数里五项数值本来就吻合，其余可传参还原；注意按数值而非名字对应档位） |
| `GlassCard`/`NormalCard` | **可以**（elevation 本就一致，颜色/圆角可传参；按压 elevation 会新增，非负面变化） |
| `TinySettingItem`（无 description） | **可以** |
| `TinySettingItem`（带 description） | **不可以**——56dp 强制变 72dp |
| `CardTabRow` → `SegmentedButton` | **不可以**——几何设计不同，是改版而非替换 |
| `TextCard` → `AssistChip` | **不可以**——32dp 高度 + 14sp 字号被强制拉大 |
| `TimePickerDialog` | **基本可以**，但会新增模式切换按钮 |

---

## 若要动手

按 A1 → A2 → A3 顺序收益最高（都是改内部实现、调用点零改动）。每步：

```bash
./gradlew :app:compileAppDebugKotlin
```

主题相关改动须在 **M3 Expressive 与 Miuix 两个引擎**下各验一遍
（`ThemeResolver.isMiuixEngine` 分支），并覆盖深/浅色。分块回归要点：

- **A1**：按钮 enabled/disabled/selected 三态配色、长按行为（书源管理、阅读器工具栏），
  以及 Miuix 引擎下 `MediumToggleButton` 不回归。
- **A2**：设置页三套行（`SettingItem`/`Tiny*`/`Compact*`）统一后行高与图标尺寸不跳变。
- **A3**：`Color.Transparent` 分支、主题「覆盖卡片圆角/描边」开关、item 九宫格背景。
- **C2**：开 TalkBack 验证朗读播放器进度条可聚焦、可读出百分比、可用音量键调节。
- **C3**：首页「最近阅读」多行时滚动流畅度（改前后对比，GPU 呈现模式或 Perfetto）。

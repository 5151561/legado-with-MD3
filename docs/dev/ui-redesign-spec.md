# UI 重设计规格（设计稿导入结果）

> 来源：Claude Design 项目「小说阅读软件设计」`eb37d848-0903-4d38-90e2-995aa6853c71`
> 导入日期：2026-08-22
> 配套：[`ui-rebuild-phase10-card.md`](./ui-rebuild-phase10-card.md)（Phase 10 线 B 的实现依据）

## 1. 导入范围

| 文件 | 大小 | 用途 |
|---|---:|---|
| `阅读 Legado 重设计 · P0 画板墙.dc.html` | 212 KB | 24 个画板 |
| `阅读 Legado 重设计 · P1 画板墙.dc.html` | 165 KB | 21 个画板 |
| `阅读 Legado 重设计 · P2 画板墙.dc.html` | 81 KB | 10 个画板 |
| `support.js` | 71 KB | **与实现无关**：Design Canvas 编辑器生成的运行时（`dc-runtime`，文件头标注 do-not-edit） |
| `画板索引.md` | — | 画板覆盖表 |
| `_ds/material-design-3-…/` | — | MD3 令牌与组件 kit（画板引用的 CSS） |

共 **55 个画板**。除 X-01（1180×820 平板横屏）与 X-02（1080×840 折叠屏展开）外，全部为 390×844 手机竖屏。

## 2. 色彩令牌

取自三面画板墙共用的 `:root`，种子色 **石墨青 `#35606E`**。已固化为
[`AppColorScheme.kt`](../../core/designsystem/src/main/java/io/legado/app/core/designsystem/theme/AppColorScheme.kt)。

### 日光（浅色）

| 角色 | 值 | 角色 | 值 |
|---|---|---|---|
| primary | `#35606E` | onPrimary | `#FFFFFF` |
| primaryContainer | `#C3E7F4` | onPrimaryContainer | `#001F28` |
| secondary | `#4C6169` | secondaryContainer | `#CFE6EE` |
| tertiary | `#6E5B3E` | tertiaryContainer | `#F6E4C9` |
| error | `#BA1A1A` | errorContainer | `#FFDAD6` |
| surface | `#F7FAFB` | onSurface | `#181C1E` |
| surfaceContainerLowest → High | `#FFFFFF` `#F1F6F8` `#EAF1F3` `#E3EBEE` | surfaceVariant | `#DCE5E8` |
| outline | `#6F787C` | outlineVariant | `#C0C9CC` |

### 夜墨（深色）

| 角色 | 值 | 角色 | 值 |
|---|---|---|---|
| primary | `#9CD0E1` | onPrimary | `#003544` |
| primaryContainer | `#1E4B58` | onPrimaryContainer | `#C3E7F4` |
| surface | `#0F1315` | onSurface | `#DDE3E5` |
| surface 阶梯 | `#191F21` `#222A2D` `#2A3336` `#323C40` | onSurfaceVariant | `#A9B4B7` |
| outline | `#899497` | outlineVariant | `#3F484B` |
| error | `#FFB4AB` | errorContainer | `#93000A` |

**已决**（2026-08-22）：设计稿未给出深色的 secondary / tertiary / onError，
按 MD3 同色族推导并确认采用，代码里逐行标注「推导值」。后续设计若补正式值，替换这几行即可。

## 3. 阅读纸色（独立于 App 主题）

画板 N-04 明确："夜墨方案下阅读页另有独立纸色，可在阅读样式抽屉里单独选择，不受此处影响。"

因此纸色是**独立调色板**，不随主题联动。已固化为
[`ReadingPalette.kt`](../../core/designsystem/src/main/java/io/legado/app/core/designsystem/theme/ReadingPalette.kt)。

| 预设 | paper | ink | inkDim |
|---|---|---|---|
| 纸（默认） | `#F4EEE3` | `#2B2823` | `#5E5951` |
| 白 | `#FFFFFF` | `#1B1B1B` | `#5A5A5A` |
| 绿 | `#E6EFE7` | `#22302A` | `#54615A` |
| 夜 | `#14181A` | `#C6C3BC` | `#8A8880` |

另有两个非选项变体：**纯黑正文背景**（设置项，OLED 省电）与**墨水屏模式**（黑白高对比 + 无翻页动画）。
「背景图」是并列的另一条分支，不属于纸色预设。

## 4. 字阶

按三面墙的实测频次定档，不套完整 MD3 字阶。已固化为
[`AppTypography.kt`](../../core/designsystem/src/main/java/io/legado/app/core/designsystem/theme/AppTypography.kt)。

| 角色 | 规格 | 出现次数 |
|---|---|---:|
| `readingBody`（正文基准） | 衬线 17px w400 lh1.95 | 32 |
| `bookTitleLarge` | 衬线 22px w500 lh1.3 | 15 |
| `chapterTitle` | 衬线 20px w500 lh1.5 | 7 |
| `coverTitle` | 衬线 12px w500 lh1.5 | 14 |
| `label` | 无衬线 13px w500 | 107 |
| `caption` | 无衬线 12px w400 lh1.3 | 80 |
| `listTitle` / `listBody` | 无衬线 15px w500 / w400 lh1.3 | 49 / 44 |
| `micro` | 无衬线 11px w400 | 71 |

**未决**：设计稿用 `Noto Serif SC` / `Noto Sans SC`，仓库未打包。当前回退到
`FontFamily.Serif`（系统 Noto Serif）与系统默认无衬线。要与设计稿逐字形一致需打包字体资产。

## 5. 主题模型

画板 N-04 给出的设置结构：

- 主题：**日光 / 夜墨 / 跟随系统** 三选一
- 按时间自动切换（19:30 – 07:00）
- 正文纯黑背景（OLED）
- **强调色**用户可选 → 配色方案是种子色驱动的，`#35606E` 是默认种子
- 界面字号

即：模式收敛为三选一，个性化通过强调色种子表达；与现有 14 种 `AppThemeMode` 的形态不同。

## 6. 画板 → feature 映射

| 前缀 | 画板 | 归属 | 状态 |
|---|---|---|---|
| B、G | B-01/B-01b/B-03/B-03b、G-01 | bookshelf | 有 api+impl |
| S、D、I、Y、P-03 | S-01/S-04*/S-06、D-01/D-02、I-01、Y-01/Y-02、P-03 | catalog | 有 api+impl |
| R | R-01/R-01a/R-01e/R-02*/R-05/R-07/R-08/R-09/R-10、R-14/R-15 | reader | 仅 api，impl 未建 |
| V、R-06 | V-01/V-02、R-06 | readaloud | 仅 api，impl 未建 |
| F | F-01/F-02 | rss | 有 api+impl |
| C、P-01 | C-01/C-02、P-01 | settings | 仅 api，impl 未建 |
| N、X | N-01…N-04、X-01/X-02 | 跨 feature（深色对照与自适应） | — |

### 设计引入了模块图未覆盖的界面

| 画板 | 内容 | 现状 |
|---|---|---|
| M-01 | 首页仪表盘 | **无对应 feature**，是新的主框架入口 |
| K-01 / K-02 | 备份与同步、冲突合并 | `backup-sync` 在目标模块图里但**从未立卡**，见 Phase 11 |
| T-01 | 阅读统计 | **无对应 feature** |
| W-01 | Web 服务与远程传书 | 现由 `:app` 的内嵌 Ktor 服务承载，**无 feature** |
| O-01 / O-02 / O-03 | 首次启动三步引导 | **无对应 feature** |

这五组是线 A 排期时必须补进去的新业务边界，不能默认它们属于已有七个 feature。

## 7. 设计契约（画板墙「适用规则」）

设计稿自带五条规则，实现必须遵守：

1. **语义色而非固定色**——组件只引用 primary / surface / surfaceContainer 1–4 / outline / error 槽位，
   不得硬编码色值；换主题包或动态色不需重画。
2. **阅读面与操作面分离**——正文用纸色 + 衬线体，菜单与抽屉用 surface + 无衬线体；
   正文对比度永不被装饰降低。
3. **状态优先于成功态**——搜索给来源进度与单源失败；阅读器异常每类给「重试 / 换源 / 编辑规则 / 停用」
   四条出路；危险操作说明数量与体积。
4. **触点与字号下限**——所有可点区域 **≥48 dp**；UI 正文 ≥14 sp，正文默认 17 sp，可放大到 26 sp 不溢出。
5. **设置路由重排**——九个平级入口改三组；每天要动的放「我的」，一次配置很久不动的放 C-01；
   同一入口不在两处出现。

规则 4 已编码进 kit：视觉高 40dp 的胶囊按钮由外层补足到 48dp 触点。

## 8. 形状与间距

| 体系 | 档位 | 依据 |
|---|---|---|
| 形状 | 2 / 4 / 8 / 12 / 16 / 18 / 20 / 24 / 28 dp + 全圆角 | 869 处 `border-radius`；16px×170、20px×91、12px×89、28px×71 |
| 间距 | 2dp 网格：2/4/6/8/10/12/14/16/20/24 | 1092 处 `gap` + 1051 处 `padding`，非 4/8 整倍数体系 |
| 控件 | 触点 48、顶栏 56、状态栏 44、按钮 40（强调 48）、chip 32、前导圆底 40、行高 ≥54、进度条 4、分隔线 1 | 实测高频值 |

## 9. 已实现

Phase 10 线 B 已落地，全部通过 `:core:designsystem:compileDebugKotlin` 与 `lintDebug`：

**令牌层** `theme/`
`AppColorScheme.kt`（日光/夜墨）、`ReadingPalette.kt`（纸色，独立于主题）、
`AppTypography.kt`（11 档字阶）、`AppShapes.kt`、`AppDimens.kt`、
`AppTheme.kt`（读取入口 + `ProvideAppTheme`，同时映射进 `MaterialTheme` 避免第二套视觉）。

**组件 kit** `kit/`

| 组件 | 规格来源 |
|---|---|
| `AppFilledButton` / `AppOutlinedButton` / `AppProminentButton` | R-01e：视觉 40dp 全圆角 / 48dp 圆角 12dp，触点补足 48dp |
| `AppTopAppBar` + `AppIconSlot` | C-01 / N-04 / M-01：56dp 高，三档标题——衬线 22sp（主界面）、衬线 20sp（二级页）、无衬线 15sp（子页） |
| `AppSectionHeader` / `AppGroupCard` / `AppSettingRow` | C-01：圆角 18dp 分组卡、行高 ≥54dp、40dp 圆底前导 |
| `AppLinearProgress` | M-01 / S-01 / C-02：厚 4dp 圆角 2dp |
| `BookCardState` + `AppBookCover` / `BookCoverPlaceholder` | B-01：四种状态角标，见下表 |
| `AppBookGridItem` / `AppBookListRow` | B-01 模式 1c（3 列 3:4）/ B-03b（行高 72dp、缩略 40×54） |
| `AppFilterChip` | B-01 / B-03b：32dp 全圆角，选中填充 secondaryContainer |
| `AppErrorCard` | R-01e：圆角 20dp、errorContainer 底，强制要求给出路 |
| `AppSegmentedControl` | B-03：高 40dp、1dp 描边、圆角 20dp、段间 1dp 竖线 |
| `AppSheetContainer` / `AppSheetHandle` / `AppSheetHeader` / `AppSheetSection` | B-03 / R-02：顶角 28dp、surfaceContainerLow、把手 32×4dp、标题 17sp |

### 书籍卡片的四种状态

设计稿「已定方向」写明均齐网格、在读优先混排、紧凑列表**共用同一套状态**，
因此统一建模为 `BookCardState`，三种布局共享：

| 状态 | 位置 | 规格 |
|---|---|---|
| 更新数 | 右上 8dp | 高 20dp、全圆角、error 底、白字 11sp |
| 来源失效 | 左上 8dp | 高 20dp、圆角 8dp、errorContainer 底、10sp |
| 已缓存 | 右下 8dp | 22dp 圆形、42% 黑蒙版 |
| 进度 | 底边 | 厚 4dp、30% 黑轨道、primaryContainer 指示 |

封面比例统一 3:4（网格 112×150、缩略 40×54）。封面图经 slot 传入，
设计系统不依赖图片加载库。

新 kit 放在 `…designsystem.kit` 包，与现有 11 个简化平行组件（`…designsystem.component`）隔离；
后者随各 feature 迁移删除。

**未实现**：空态容器（设计稿未找到明确空态画板，不凭空造）、书籍详情与目录专用组件、
自适应断点（X-01 平板三栏 / X-02 折叠屏双栏）、无障碍与大字体走查。

**两处按设计稿保留的 40dp 控件**：分段控件与胶囊按钮视觉高均为 40dp。按钮已由外层补足到 48dp 触点；
分段控件各段等分平铺、无误触间隙，按设计稿保持 40dp，单独使用某一段时需调用方自行补足。


## 10. 已落地的界面

| 画板 | 位置 | 状态 |
|---|---|---|
| C-01 设置主页 | `feature/settings/ui/SettingsHomeScreen.kt` + `SettingsHomeContract.kt` | 无状态 Screen + 契约 + 日光/夜墨双预览，编译与 lint 通过 |

C-01 是重设计的第一个真实界面，用来检验 kit 的取值与 API。它按项目规范做成无状态 Screen：
只接收 `state` 与 `onIntent`，不注入 ViewModel、不读偏好。数据装配等 `settings:api`
扩面后由 ViewModel 提供（Phase 10 线 A），当前用画板原始数据驱动预览。

**对稿时发现并修正的偏差**：顶栏衬线标题在设计稿里有 22sp（M-01 首页、B-01 书架）与
20sp（C-01 设置）两档，最初统一套用了 22sp。已拆为 `displayTitleStyle` 与
`sectionTitleStyle` 两个默认值，并注明不可混用。

### 截图基线

已接入 Roborazzi 1.72.0（`:feature:settings:ui`），把「实现与设计稿一致」变成可执行断言：

```bash
./gradlew :feature:settings:ui:recordRoborazziDebug   # 记录基线
./gradlew :feature:settings:ui:verifyRoborazziDebug   # 校验差异
```

基线图提交在 `feature/settings/ui/src/test/screenshots/`，渲染确定（两次 record 字节一致），
改动任一令牌都会被检出。

两项限制：

- **SDK 固定为 36**。项目 targetSdk 为 37，Robolectric 4.16.1 最高支持 36，
  不固定会以「targetSdkVersion=37 > maxSdkVersion=36」失败。
- **无法验证 window inset**。Robolectric 下系统栏 inset 为 0，
  edge-to-edge 的状态栏/导航栏让位仍需真机确认。

### C-01 与设计稿的三处差异（待线 A 解决）

| 元素 | 现状 | 原因 |
|---|---|---|
| 顶栏搜索 | **未呈现** | 设计稿只画了图标，没有设置搜索页；现有 App 也无可接实现。真搜索需 `settings:api` 提供全量设置项索引（标题、所属页、跳转路径），只过滤七个入口没有意义。契约保留 `OpenSearch`，扩面时接上 |
| 朗读与听书 | 点击无跳转 | 现有导航无对应目的地，是重设计新引入的界面 |
| Web 服务与设备 | 点击无跳转 | 同上，对应画板 W-01，`:app` 内嵌 Ktor 服务尚未 feature 化 |

各入口摘要文案当前取自画板示例，不反映真实设置——`settings:api` 仅 16 行，尚未表达任何设置摘要。

### edge-to-edge

App 在 `BaseActivity` / `BaseComposeActivity` 调用 `enableEdgeToEdge()`，内容绘制到系统栏之下。
处理方式：

- `AppTopAppBar` 自行消费 `WindowInsets.statusBars`（与 Material 3 `TopAppBar` 约定一致），
  用它的屏幕自动正确。
- 屏幕底部滚动容器需自行加 `WindowInsets.navigationBars` 底部内边距。
- 底部面板的 inset 归宿主，`AppSheetContainer` 不重复处理。
- **不提供状态栏/导航栏高度常量**：画板里的 44dp 只是稿面模拟，真机随设备与显示模式变化。

**尚未处理**：横屏 `displayCutout` 与输入法 `ime` inset。

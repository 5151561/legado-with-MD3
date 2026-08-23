# Catalog 行为盘点（Phase 10 线 A 第一步）

> 日期：2026-08-23
> 上游：[`ui-rebuild-phase10-card.md`](./ui-rebuild-phase10-card.md) 第 4 节「删除前必须先有契约」
> 覆盖画板：D-00 源与规则枢纽、S-04/S-04a 书籍详情、S-06a 阅读器内目录、S-06b 目录管理

本文是 `feature:catalog` 扩面的依据。第 3 节逐块列出旧 UI 的行为与它在新契约里的落点；
第 4 节是**未覆盖登记**——旧 UI 里存在、四块画板没有接住的行为，删除旧 UI 前必须逐条销账；
第 5 节列出 api 需要而底层尚不存在的数据能力；第 6 节是三个必须由人决定的问题。

## 1. 方法

对每个旧页面读 Contract / ViewModel 的 `Intent` 与 `UiState`，逐条判定：

- **已覆盖**：新契约里有对应的 Intent 或状态字段。
- **移交**：行为仍在，但归属另一块画板（如换源归 S-08、导入归 P-08）。
- **未覆盖**：新契约里没有落点。这一类**不是"可以删"**，而是删除门禁的欠账。
- **新增**：新契约有、旧 UI 没有的行为，需要新建能力。

## 2. 旧 UI 清单

| 画板 | 旧实现 | 规模 |
|---|---|---:|
| D-00 源与规则枢纽 | 无对应页；九类入口散在设置、「我的」与阅读器 sheet 中 | — |
| — 书源 | `ui/book/source/manage/` | 1947 |
| — 订阅源 | `ui/rss/source/manage/` | — |
| — HTTP TTS | `ui/book/readaloud/cloudtts/` | — |
| — 替换净化 | `ui/replace/` | — |
| — TXT 目录规则 | `ui/book/toc/rule/` | — |
| — 词典规则 | `ui/dict/rule/` | — |
| — 正文高亮 | `ui/book/read/sheet/HighlightRuleConfigSheet.kt`（阅读器内 sheet） | — |
| — 标签高亮 | `ui/highlightTagRule/` | — |
| — 规则订阅 | `ui/rss/subscription/` | — |
| S-04 书籍详情 | `ui/book/info/` | 4588 |
| S-06a/S-06b 目录 | `ui/book/toc/` | 2438 |
| S-06b 批量缓存 | `ui/book/cache/manage/` + `ui/book/cache/CacheAdapter.kt` | 1758 |

D-00 是**新增页**，没有一个旧页面与之对应——它要做的是把九个各自独立的管理页收敛成一个带
数量摘要的枢纽。因此 D-00 的盘点对象不是「某个旧页面的行为」，而是「九个入口各自的计数口径」。

## 3. 逐块盘点

### 3.1 D-00 源与规则枢纽

九类入口全部只需要**计数投影**，管理页本身不在本次范围（点进去仍走旧页面）。

| 入口 | 数据源 | 计数口径 | 判定 |
|---|---|---|---|
| 书源 | `BookSourceDao` | 总数 / 启用数 / 失效数 | 前两项已覆盖；失效数见 §6.1 |
| 订阅源 | `RssSourceDao` | 总数 / 启用数 | 已覆盖 |
| HTTP TTS | `HttpTTSDao` | 总数 / 默认引擎名 | 默认引擎名见 §6.2 |
| 替换净化 | `ReplaceRuleDao` | 总数 / 启用数 | 已覆盖 |
| TXT 目录规则 | `TxtTocRuleDao` | 总数 / 内置条数 | 已覆盖 |
| 词典规则 | `DictRuleDao` | 总数 | 已覆盖 |
| 正文高亮 | `HighlightRuleDao` | 总数 / 启用数 | 已覆盖 |
| 标签高亮 | `HighlightTagRuleDao` | 总数 / 启用数 | 已覆盖 |
| 订阅规则 | `RuleSubDao` | 总数 | 命名歧义见 §6.3 |

导入（`SourceHubIntent.OpenImport`）是**移交**：粘贴 / 文件 / 扫码 / 内置库四条来源统一进画板
P-08 的审核面板，本次只出导航，不进 catalog api。

### 3.2 S-04/S-04a 书籍详情（对旧 `ui/book/info/`）

旧 `BookInfoIntent` 共 30 条、`BookInfoMenuAction` 共 18 项。判定如下。

**已覆盖**

| 旧行为 | 新落点 |
|---|---|
| `ReadClick` 开始/继续阅读 | `BookDetailIntent.ContinueReading` |
| `ShelfClick` 加入/移出书架 | `BookDetailIntent.SelectMenuAction(RemoveFromShelf)` + 加入走书架侧 |
| `ConfirmDelete(deleteOriginal)` | `BookDetailDialog.RemoveFromShelf` + `SetDeleteLocalFile` |
| `TocClick` 进目录 | `BookDetailIntent.OpenEntry(Catalog)` |
| `GroupClick` / `SelectGroup` | 菜单项 `MoveToGroup` |
| `CoverClick` / `SelectCover` / `SaveCover` | 菜单项 `ChangeCover` |
| `RemarkClick` / `UpdateRemark` | 菜单项 `Note` |
| `MenuAction(Edit)` 编辑书籍信息 | 菜单项 `EditInfo` |
| `MenuAction(SetBookVariable / SetSourceVariable)` | 菜单项 `EditVariables` |
| `MenuAction(Share)` | `BookDetailIntent.Share` |
| `RelatedBookClick` / `RelatedBooksMore` | `BookDetailIntent.OpenRelated` |
| `CharacterListClick` / `KnowledgeListClick` / `EventListClick` | `OpenEntry(Insights)`，三个计数合并成一行 |
| 简介展开 | `ToggleIntro` |
| `bookSource` 展示 | `BookSourceSummaryUi` |

**移交**

| 旧行为 | 去向 |
|---|---|
| `ChangeSourceClick` / `ReplaceWithSource` / `AddSourceAsNewBook` / `ReplaceConflictingBook` | 画板 S-08 统一换源组件；详情只出 `ChangeSourceIntent` 与候选数 |
| `SelectWebFile` / `OpenUnsupportedWebFile` / `SelectArchiveEntry` / `SetDefaultBookTreeUri` | 导入流程（画板 P-08） |
| `MenuAction(Login)` / `OriginClick` / `CustomButton` | 书源管理与登录，归 D-00 下游 |

**未覆盖**——见 §4.1。

**新增**：`BookDetailIntent.ListenAloud`。旧详情页**没有**听书入口（`ui/book/info/` 全目录无
`AudioPlay` 调用）。这是重设计新增的行为，落点在 `readaloud`，不进 catalog api，
详情页只负责导航出 `bookId`。

### 3.3 S-06a 阅读器内目录（对旧 `ui/book/toc/`）

新契约明确「只做跳转 + 单章操作」，旧 `TocIntent` 的批量部分整体移交 S-06b。

| 旧行为 | 判定 |
|---|---|
| `LoadBook` / 目录列表 | 已覆盖（`ReaderTocUiState.chapters`） |
| 当前章高亮、已读标记、章内进度 | 已覆盖（`isCurrent` / `isRead` / `progress`） |
| `ReverseToc` | 已覆盖（`ToggleOrder`） |
| `SetSearchMode` / `SetSearchQuery` | 已覆盖（`Search`，搜索态由上层承载） |
| `DownloadChapter(id)` | 已覆盖（`SelectChapterAction(DownloadChapter)`） |
| `DownloadAll` / `DownloadSelected` / `SelectAll` / `InvertSelection` / `ClearSelection` / `SelectFromLast` / `ToggleSelection` | 移交 S-06b |
| `ToggleVolume` / `ExpandAllVolumes` / `CollapseAllVolumes` | **未覆盖**，见 §4.2 |
| `ExportBookmarks` / `UpdateBookmark` / `DeleteBookmark` / `AddBookmarksForSelected` / 书签与笔记两个 Tab | **未覆盖**，见 §4.2 |
| `SaveTocRegex` / `UpdateToc` / `ToggleSplitLongChapter` | **未覆盖**，见 §4.2 |
| `ToggleUseReplace` / `ToggleShowWordCount` | **未覆盖**，见 §4.2 |

### 3.4 S-06b 目录管理（对旧 `ui/book/toc/` 批量部分 + `ui/book/cache/manage/`）

| 旧行为 | 判定 |
|---|---|
| 章节多选、全选、反选 | 已覆盖 |
| `SelectFromLast` 起止范围选择 | 已覆盖（`SelectRange`，交互改为长按首章 → 点尾章） |
| `DownloadSelected` / `DownloadAll` | 已覆盖（`DownloadSelected`；全部下载 = 全选后下载） |
| `DeleteChapterCache` | 已覆盖（`DeleteSelectedCache`，收在「更多」并二次确认） |
| 单章重试 | 已覆盖（`RetryChapter`） |
| 章节缓存状态（未缓存 / 已缓存 / 等待 / 下载中 / 暂停 / 失败 + 进度） | 已覆盖，口径沿用 `resolveChapterCacheStatusKey`，但新契约只暴露三态，见 §6.4 |
| 按缓存状态过滤 | **新增**（旧 UI 无过滤器）；`TocFilter.All / NotCached / Failed` |
| `StopBookDownload` / `StopAllDownloads` / `StopChapterDownload` | **未覆盖**，见 §4.3 |
| `StartAllDownloads` 跨书批量 | 移交：跨书缓存管理是独立页面，不属于单书目录管理 |
| `DeleteBookCache` 整本删缓存 | 移交：等价于「全选 + 删除缓存」 |
| 书籍分组筛选、排序（`BookCacheListSort`） | 移交：属于跨书缓存管理页 |

## 4. 未覆盖登记（删除门禁清单）

> **状态变更（2026-08-23）**：产品决定不等销账，直接切换并接受功能暗掉。
> `ui/book/info/` 的旧详情页已删除（`BookInfoActivity` / `BookInfoScreen` /
> `BookInfoViewModel` / `BookInfoContract` / `BookInfoRouteScreen` /
> `BookInfoReadRecordSheet` / `BookInfoBackdropStyle`，合计约 4300 行），
> `MainRouteBookInfo` 现在渲染画板 S-04 的新详情页。
> `BookInfoSheets.kt` 保留——它的 `GroupSelectSheet` 与 `ChangeSourceSheet` 被书架在用；
> `HighlightedTag` 与 `READER_RESULT_DELETED` 移到
> `ui/widget/components/card/HighlightedTag.kt`。
>
> **本节因此不再是"删除前的门禁"，而是"已经暗掉、待补回"的清单。**
> `ui/book/toc/` 与 `ui/book/cache/manage/` 尚未删除，见第 7 节。

### 4.1 书籍详情

| 欠账 | 说明 |
|---|---|
| `MenuAction(Refresh)` | 刷新书籍信息与目录。高频功能，新画板上没有入口 |
| `MenuAction(Upload)` / `SyncRemote` | WebDAV 上传与远程同步 |
| `MenuAction(Top)` | 置顶 |
| `MenuAction(ToggleCanUpdate)` | 允许更新开关 |
| `MenuAction(ToggleSplitLongChapter)` | 拆分长章节 |
| `MenuAction(ToggleDeleteAlert)` | 删除确认开关 |
| `MenuAction(ClearCache)` | 清除本书缓存（S-06b 的「删除缓存」是按选中章节，不等价） |
| `MenuAction(CopyBookUrl)` / `CopyTocUrl` | 复制链接 |
| `MenuAction(ShowLog)` / `showAppLogSheet` | 调试日志 |
| `ReadRecordClick` + `BookInfoReadRecordSheet`（167 行） | 本书阅读记录与时间线 |
| `AuthorClick` / `BookNameClick`（含长按） | 按作者、按书名搜索 |
| `highlightedTags` | 书架标签高亮在详情页的呈现 |
| `AddCharacterClick` / `CharacterClick` / `CharacterNetworkClick` | 人物详情与人物关系网；新画板只保留一个合并入口与计数 |
| 相关推荐（`relatedBooks`） | api 已表达，**impl 未接线**：数据来自书源 `ruleBookInfo.relatedBooks` 规则 + 网络，唯一实现是 `BookInfoViewModel` 的私有加载逻辑，需先抽成可复用用例。`AppCatalogRelatedBooksHost` 当前恒返回空列表 |

### 4.2 目录

| 欠账 | 说明 |
|---|---|
| 卷折叠（`ToggleVolume` / 展开全部 / 折叠全部 + `TocHierarchy.kt` 86 行的两套层级算法） | 多卷书与多层级目录的核心交互，新契约是平铺列表 |
| 书签 Tab（`TocBookmarkItemUi`、增删改、导出 Markdown） | 整个 Tab 无落点 |
| 笔记 / 划线 Tab（`TocMarkingItemUi`、跨源笔记标记） | 整个 Tab 无落点 |
| `SaveTocRegex` + `ui/book/toc/rule/` 预览 | 本地 TXT 书的目录正则调试 |
| `UpdateToc` 刷新目录 | |
| `ToggleUseReplace` / `ToggleShowWordCount` / `titleReplaceProgress` | 目录标题净化与字数显示 |
| VIP / 付费章标记（`isVip` / `isPay`） | 新契约的 `TocChapterStatus` 只有三态 |

### 4.3 缓存

| 欠账 | 说明 |
|---|---|
| 停止下载（整本 / 单章 / 全部） | 新契约只有「下载」，没有「停止」。长任务不可中止是回归 |
| 暂停态与「有暂停任务」提示 | |
| 已缓存文件数与章数不一致的呈现（`cachedFileCount`） | 图片类书籍的缓存完整性 |

## 5. api 需要而底层尚不存在的能力

| 能力 | 盘点时的现状 | 落地情况 |
|---|---|---|
| 书源失效计数 | 初判「不落库」有误，见 §6.1 | 已做：按 `getInvalidGroupNames()` 同规则计数，无需迁移 |
| 九类源与规则的计数 | 九个 DAO 各查各的，无横截面 | 已做：`SourceCatalogDao.flowCounts()` 一次子查询取齐，Room 跟踪每张表的失效 |
| 缓存体积（"18.2 MB"） | `BookHelp` 无体积聚合 | 已做：`AppCatalogChapterCacheHost.cachedBytes` 遍历书籍缓存目录求和，取不到返回 null |
| 书签条数 | `BookmarkDao` 无 count 查询 | 已做：`countByBook(name, author)` |
| 笔记条数 | `BookMarkingDao` 无 count 查询 | 已做：`countByBook(name, author)`，同样按「书名 + 作者」跨源 |
| 候选源数量 | 无查询 | 已做：`SearchBookDao.countEnabledSources`，按源去重、只数启用的书源 |
| 人物 / 知识 / 事件条数 | `BookKnowledgeDao` 无 count 查询 | 已做：三条 count 查询，人物沿用列表口径排除已删除记录 |
| 章节缓存状态投影 | 分散在 `BookHelp`（磁盘）与 `CacheBook`（内存队列） | 已做：`CatalogChapterCacheHost`，口径与 `BookCacheManageViewModel` 一致 |
| 默认 HTTP TTS 引擎名 | 偏好项，`:core:preferences` 未建 | 已做：`CatalogReadAloudPreferencesHost`，见 §6.2 |
| 章节失败原因 | 队列只记「哪些章失败」，不记为什么 | **未做**，`ChapterFailureReason` 恒为 `Unknown`。要分因由需队列携带错误类型 |
| 连载状态 | `Book` 无独立字段，旧 UI 由 `kindLabels` 拼出 | 沿用拼装，`BookDetailSnapshot.kinds` 原样给出 |

## 6. 待决问题

### 6.1 书源"失效"计数 —— 已解决（2026-08-23）

**盘点初稿的判断有误，此处更正。** 当时看到 `BookSourceItemUi.checkMessage` 来自内存态
`checkGateway.state`，据此判定失效信号不落库。不落库的只是**校验的详细消息**；
失效**标记本身是落库的**——`BookSourceCheckRepository.checkSource()` 在失败时调用
`source.addGroup("搜索失效")` / `"网站失效"` / `"js失效"` / `"校验超时"` 等，
写进 `bookSourceGroup` 后 `updateSources(source)` 持久化，判定规则就是
`BookSource.getInvalidGroupNames()`：分组名里带「失效」或等于「校验超时」。

因此不需要加字段，也不需要迁移。`SourceCatalogDao.flowCounts()` 直接按同一规则计数。

口径说明：**0 的含义是「没有书源被标记失效」，不是「全部校验通过」**——从未校验过的
书源不带任何标记。`unhealthy` 保留可空是为其他类别用（它们没有健康信号），书源侧恒有值。

### 6.2 HTTP TTS 的"默认引擎" —— 已解决（2026-08-23）

经 `CatalogReadAloudPreferencesHost` 读取，转发给既有的 `ReadAloudSettingsGateway`。
`:core:preferences` 建立后本接缝删除。

一个要点：`ReadAloudSettings.ttsEngine` 一个字段承载三种引擎的选择——系统引擎与云引擎
存 JSON，HTTP TTS 存引擎 id 原文。接缝原样透出 id，由 Room 查 `httpTTS` 表；
选的是别的引擎时自然查不到，`defaultName` 为 null，摘要里就没有「默认 X」这一段。

### 6.3 "订阅规则"指的是 `RuleSub` —— 已解决（2026-08-23）

`SourceHubEntryId.RssRules` 已改名 `RuleSubscription`，与 `SourceCatalogKind.RuleSubscription`
对齐。**只改了 enum，界面上的标题仍是「订阅规则」**——画板作者写了「与「订阅源」的区别见页首说明」
这句摘要，说明这个歧义是知情且用文案处理的，改标题是设计的决定，不在本次范围。

### 6.4 章节状态三态 vs 旧六态

新 `TocChapterStatus` 只有 `Cached / NotCached / Failed`，旧口径有六种
（含等待、下载中、暂停）。目录管理页要显示下载进度，三态不够用。
api 侧保留完整状态，由 UI 决定折叠成几档——这样 §4.3 的「停止下载」将来补回时，api 不用改。


## 7. 切换后的实际状态（2026-08-23）

### 已切换

| 路由 | 现在渲染 | 旧实现 |
|---|---|---|
| `MainRouteBookInfo` | 画板 S-04 `BookDetailScreen` | 已删除 |
| `MainRouteTocManage`（新增） | 画板 S-06b `TocManageScreen` | 无（新页） |
| `MainRouteSourceHub`（新增） | 画板 D-00 `SourceHubScreen` | 无（新页） |

### 当场暗掉的功能

详情页上这些动作现在只弹「该功能正在重做，暂不可用」：
**换源、移动到书组、换封面、变量编辑、备注、分享、听书**。
它们在旧实现里是 `BookInfoRouteScreen` 内部的 sheet，新页面没有对应实现。
相关推荐恒为空列表。第 4.1 节登记的 13 项旧菜单（刷新、WebDAV 上传、置顶、清缓存、
复制链接、阅读记录、按作者搜索……）随旧页面一并消失。

另有一处链路断开：阅读器原先通过 Activity Result 得知「书在详情页被删了」
（`ReadBookIntent.BookInfoResult`），改成路由导航后不再回传结果，
该 Intent 现在没有任何发送方。

### 未切换

- **阅读器内目录**（画板 S-06a）尚未接入。`ReadBookRouteScreen` 仍用
  `TocActivityResult` 启动 `TocActivity`，因此 `ui/book/toc/` 整个目录保留。
  接入的障碍是跳章：新契约的 `ReaderTocEffect.JumpToChapter` 只带 chapterId，
  而阅读器要的是章节序号（`ReadBookIntent.OpenChapterResult(index, pos)`），
  需要在 effect 里补上 index。
- **跨书缓存管理**（`ui/book/cache/manage/`）不在四块画板范围内，原样保留。
- **发现 tab** 与 catalog 的迁移登记表无关变更：`catalog.legacyUiPath` 登记的是
  `ExploreScreen`，实验版 `CatalogScreen` 是它的灰度对照，这条线未动。

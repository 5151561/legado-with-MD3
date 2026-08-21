# 书架 Compose 模块化迁移卡（Phase 0）

> 状态：已完成基线盘点；尚未改动生产调用链
> 日期：2026-08-21
> 对应计划：[Compose UI 重做与业务模块化迁移计划](./compose-ui-module-migration-plan.md)
> 下一步：M1.1（Design System 接缝），不是直接移动书架代码

## 1. 范围、假设与兼容底线

本卡覆盖首批书架用户旅程：主界面书架页、分组切换/管理、选择态、书架管理页中的删除与手动排序、书架导入入口、打开书籍、阅读进度投影，以及这些路径的返回栈和恢复行为。

本卡只定义后续边界，不创建 `feature:bookshelf` 模块，不改变生产代码。以下内容在后续迁移卡未明确标记前一律保持：

- Room schema、数据库名、备份/书架 JSON 格式；
- `MainActivity` 的公开 Intent extra、根 Navigation 3 back stack 和旧入口；
- `CacheBookService`、`ExportBookService`、书源回调与 EventBus 协议；
- 本地书删除时“是否删除原文件”的用户选择；
- 书架排序、分组位掩码、阅读进度和未读数的现有含义；
- 旧页面继续使用现有 Repository/UseCase；新页面不得形成第二条写入路径。

Phase 0 没有“有意改变产品行为”或“移除”项。表中未单独说明的行为均按“保持”处理；“等价简化”只表示后续允许在契约测试保护下缩短实现，不授权改变用户可观察结果。

## 2. 当前入口与恢复基线

| 入口/场景 | 当前实现 | 当前可观察行为 | 分类 | 后续边界 |
|---|---|---|---|---|
| 主界面书架 | [`MainScreen.kt`](../../app/src/main/java/io/legado/app/ui/main/MainScreen.kt) 内的 `BookshelfRouteScreen` | 书架是 `MainRouteHome` 内的主分页；点书打开阅读/漫画/音频，长按进入书籍详情 | 保持 | `:app` 保留根导航策略；feature 只通过回调/effect 请求跳转 |
| 本地/远程导入 | `MainRouteImportLocal` / `MainRouteImportRemote` | 从书架菜单入栈；返回后回到 `MainRouteHome` | 保持 | 导入业务归 `catalog:api`；书架只发导航 effect |
| 书架管理 | `MainRouteCache(groupId)`（历史命名） | 从 Home/书籍详情入栈；从无关路由进入时重建为 Home + 管理页 | 保持 | 路由名可在兼容期保留；删除/分组/排序命令归 `bookshelf:api` |
| 打开书籍 | `MainRouteReadBook` / `MainRouteReadManga` 或旧音频入口 | 从书架打开；返回仍到 Home | 保持 | 书架 effect 只携带稳定 `bookId/bookUrl`，由 app shell 决定具体 reader route |
| Activity 重建/进程恢复 | `rememberNavBackStack` + 可序列化 `NavKey`；书架从 Room Flow 和设置重建 | 根 route/back stack 由 Navigation 3 保存；选中分组由 `saveTabPosition` 恢复 | 保持 | route 继续可序列化；API snapshot 必须可从 SSOT 重新获得 |
| 书架临时状态恢复 | ViewModel 普通 `StateFlow`，未使用 `SavedStateHandle` | 搜索、选择、编辑、overlay、拖拽和刷新进度不是持久业务事实，进程死亡后允许重置 | 保持（本批） | 新 UI 仍不把这些状态写入数据库；是否用 `SavedStateHandle` 另立产品变更卡 |

导航基线由 `MainNavigatorBookshelfTest` 固定 Home、导入、管理页的入栈/去重/重建规则，以及 `MainRouteCache.groupId` 的序列化往返。

## 3. 旧行为到新契约的核心映射

建议名称用于约束 M2，不代表 Phase 0 已创建这些类型。

| 旧行为 | 分类 | 新 Intent / Effect | 业务 API 或状态来源 | 验收要点 |
|---|---|---|---|---|
| 首次订阅分组与书籍；500 ms 后结束初始占位 | 保持；错误表达在新 UI 等价补全 | `Start`、`RetryLoad` | `observeBookshelf(query): Flow<BookshelfQueryState>` | loading/content/empty/error 可区分；失败不能伪装为空书架 |
| Room 书籍、分组或阅读进度变化后自动刷新 | 保持 | 无一次性 effect | 同一 `observeBookshelf` SSOT | UI 不手工追加/删除第二份列表 |
| 切换分组并保存 `saveTabPosition` | 保持 | `SelectGroup(groupId)` | `BookshelfSettingsGateway`（迁移后经 feature 接缝） | 切组清选择/拖拽；重启恢复分组 |
| 隐藏空分组，但永不隐藏“全部” | 保持 | 设置 intent 与查询参数变化 | snapshot 的 groups/counts | 系统组和用户组计数语义不变 |
| 搜索当前分组；可转全局搜索 | 保持 | `SetSearchMode`、`SetSearchQuery`；`NavigateToGlobalSearch` | 本地 UI 派生；全局搜索归 `catalog:api` | 当前分组筛选不触发数据库写入 |
| 文件夹模式进入分组；返回先回文件夹根 | 保持 | `OpenGroup`、`BackPressed` | UiState | predictive back 的目标与普通 back 一致 |
| 进入/退出选择态、全选、反选 | 保持 | `EnterSelection`、`ToggleSelection`、`SelectAllVisible`、`InvertVisibleSelection`、`BackPressed` | ViewModel 临时状态 | 返回先清选择，再退出编辑态 |
| 移动所选书籍到分组 | 保持 | `MoveBooks(bookIds, groupId)`；失败 `ShowMessage` | `BookshelfCommands.moveBooks` | 写入完成后只等待 SSOT 回流；空集合为 no-op |
| 手动拖拽排序 | 保持；排序实现允许等价合并 | `BeginReorder`、`MoveReorder`、`CommitOrder(bookIds)` | `BookshelfCommands.reorderBooks` | 仅手动排序模式可拖拽；失败回到 SSOT 顺序，不双写 |
| 在书架管理页删除书籍 | 保持；部分失败语义补全 | `ConfirmDelete(bookIds, deleteOriginal)`；结果消息 | `BookshelfCommands.deleteBooks` | 本地/网络书回调、章节与书籍删除仍由单一命令协调；重复点击不重复写 |
| 新建、编辑、删除、重排分组 | 保持 | `CreateGroup`、`UpdateGroup`、`DeleteGroup`、`ReorderGroups` | `BookshelfGroupCommands`（仍属于 `bookshelf:api`） | 位掩码与删除分组后的书籍处理不变 |
| 从 JSON/URL 导入书架 | 保持 | `RequestImportDocument`、`ImportDocumentPicked` 或 `NavigateToImport` | 业务归 `catalog:api`；Activity Result 归 Route | 书架 feature 不接收 `Uri` 作为业务模型，也不直接读文件 |
| 进入本地/远程导入页 | 保持 | `NavigateToLocalImport` / `NavigateToRemoteImport` | app shell + catalog route | 旧 Intent 入口继续可用 |
| 更新目录、自动刷新、预下载 | 保持；所有权后续下沉 | `RefreshBooks(bookIds)` | 现有 `RefreshTocUseCase`/缓存兼容桥；目标归 catalog 的内容获取边界 | EventBus/Service 协议本阶段不变，不在新 UI 复制队列 |
| 展示阅读章节、位置、时间、未读数 | 保持 | 无写 Intent | `BookshelfBookSummary.readingProgress` 只读投影 | reader 写入后由 Room SSOT 回流 |
| 写入阅读进度 | 保持旧路径，不纳入书架命令 | 无书架 Intent | 唯一所有者为未来 `reader:api` | `bookshelf:api` 永远不提供保存阅读进度命令 |
| 布局、排序、刷新与标签颜色设置 | 保持 | `UpdateBookshelfPreference` / `UpdateTagColors` | settings 所有者；书架只消费投影 | 相关多字段更新仍使用一次 `update { copy(...) }` |
| 导出/上传书架 | 保持旧路径 | `RequestExportDocument`、`ExportDocumentCreated`、`UploadBookshelf` | 目标归 `backup-sync:api`；Activity Result/剪贴板归 Route | 格式不变；上传成功 URL 是 effect，不放长期 UiState |
| 成功/失败提示 | 保持视觉语义 | `ShowMessage` | Route 唯一收集 `BookshelfEffect` | 同一 effect 流只有 Route 一个收集者 |

## 4. 当前依赖盘点与承接边界

### 4.1 数据、Repository 与 UseCase

| 当前依赖 | 当前用途 | 目标承接 |
|---|---|---|
| `BookRepository` | 分组书籍/计数/预览 Flow、读取完整 Book、排序写入 | 查询映射进 `BookshelfQuery`; 删除/分组/顺序写入进 commands；不得向 UI 暴露 Room `Book` |
| `BookGroupRepository` | 可见/全部分组 Flow 与分组详情 | `BookshelfQuery` / `BookshelfGroupCommands` |
| `BookshelfRepository` | 对 UI `BookShelfItem` 排序 | 排序规则下沉到合法实现并与管理页合并；正式 impl 不得依赖 UI 类型 |
| `BookSourceRepository` | 目录刷新和管理页换源 | catalog/换源边界；不进入首批书架 UI API |
| `UploadRepository` | 上传书架 JSON | `backup-sync:api`；迁移前留在 app 兼容适配器 |
| `UpdateBooksGroupUseCase` | 批量替换书籍分组 | `BookshelfCommands.moveBooks` 的现有实现接缝 |
| `DeleteBooksUseCase` | 删除原文件/回调、章节与书籍 | `BookshelfCommands.deleteBooks` 的单一实现接缝 |
| `AddBookUseCase` / `ImportBookshelfUseCase` | URL/JSON 导入 | `catalog:api` |
| `RefreshTocUseCase` / `BatchCacheDownloadUseCase` | 更新目录与缓存 | catalog 内容获取边界；首批可保留 app 兼容桥 |
| `ExportBookshelfUseCase` | 文件/JSON 导出 | `backup-sync:api`；先移除其对 `BookUiItem` 的反向依赖 |

当前明确存在 Repository/Domain → UI 的反向依赖：`BookRepository`、`BookshelfRepository`、`SearchRepository` 使用 `BookShelfItem`，`ExportBookshelfUseCase` 使用 `BookUiItem`。这些是创建正式 `feature:bookshelf:impl` 前必须解除的边界阻断项，不能用 `impl -> :app` 绕过。

### 4.2 全局模型、Service、EventBus 与系统边界

| 当前对象 | 当前用途 | Phase 0 决定 |
|---|---|---|
| `CacheBook`、`CacheBookService` | 目录刷新后的预下载、管理页缓存队列 | 保持协议；新 Screen 不直接访问，由 app/业务适配器桥接 |
| `SourceCallBack` | 书架刷新结束通知 | 保持；与刷新命令一起迁出 ViewModel，不复制回调 |
| `ExportBookService` | 管理页导出状态和启动服务 | 保持；由 Route/业务边界承接，不进入书架 render model |
| `FlowEventBus(UP_ALL_BOOK_TOC)` | 外部触发全书架目录刷新 | 保持兼容订阅；SSOT 仍是 Room Flow，EventBus 不是书架列表状态源 |
| `postEvent(UP_BOOKSHELF)` | 更新开始/结束兼容通知 | 保持到所有消费者迁移完；不得与新 API 双写列表状态 |
| `OpenDocument` / `CreateDocument` / `OpenDocumentTree` | 导入、导出文件结果 | 目标由 `BookshelfRouteScreen` 唯一持有 launcher，并转换为 host effect/命令参数 |
| 导航 callbacks | 打开 reader、详情、搜索、导入、管理 | 继续由 app shell 收集并修改根 back stack |
| clipboard/toast/snackbar | 提示与上传链接复制 | 目标由 Route 的单一 effect 收集器处理 |

### 4.3 设置项

- `BookshelfSettingsGateway`：`saveTabPosition`、`bookGroupStyle`、隐藏空分组、默认/分组排序、刷新限制、自动刷新、等待更新数，以及书架列表/网格/文件夹布局和可见信息字段。
- `ThemeSettingsGateway`：模糊抬高底部间距、自定义标签颜色、主题色。
- `BookExportSettingsGateway`：管理页导出类型、编码、替换净化、WebDAV、图片及自定义导出字段。
- 兼容全局：`AppConfig.threadCount/preDownloadNum`、`LocalConfig.deleteBookOriginal`、`BookshelfManageScreenConfig`。这些不能出现在新 feature UI；分别由业务/设置接缝提供快照或命令。
- `BookshelfViewModel` 当前组合了 `AppShellSettingsGateway.settings` 但未使用该值，属于可在迁移时净删除的无效依赖，不在 Phase 0 改生产代码。

## 5. `bookshelf:api` 最小草案（M0.2）

API 模型不使用 Compose、`Uri`、`Context`、Room entity、DAO、Service 或可变运行时对象。ID 首批继续使用稳定 `bookUrl`，以保持兼容。

```kotlin
interface BookshelfQuery {
    fun observeBookshelf(request: BookshelfQueryRequest): Flow<BookshelfQueryState>
}

interface BookshelfCommands {
    suspend fun moveBooks(bookIds: Set<String>, groupId: Long): BookshelfCommandResult
    suspend fun reorderBooks(groupId: Long, orderedBookIds: List<String>): BookshelfCommandResult
    suspend fun deleteBooks(
        bookIds: Set<String>,
        deleteOriginal: Boolean,
    ): BookshelfCommandResult
}

interface BookshelfGroupCommands {
    suspend fun createGroup(draft: BookshelfGroupDraft): BookshelfCommandResult
    suspend fun updateGroup(group: BookshelfGroup): BookshelfCommandResult
    suspend fun deleteGroup(groupId: Long): BookshelfCommandResult
    suspend fun reorderGroups(orderedGroupIds: List<Long>): BookshelfCommandResult
}
```

建议查询状态：

```kotlin
sealed interface BookshelfQueryState {
    data object Loading : BookshelfQueryState
    data class Data(
        val snapshot: BookshelfSnapshot,
        val warnings: List<BookshelfIssue> = emptyList(),
    ) : BookshelfQueryState
    data class Failure(
        val error: BookshelfError,
        val previous: BookshelfSnapshot? = null,
    ) : BookshelfQueryState
}
```

`Data` 中书籍为空且查询成功就是空内容；`Failure.previous != null` 表示可继续展示旧内容的失败。ViewModel 映射为互斥的 `initialLoading / content / empty / fullError`，并可在 content 上叠加 `refreshing` 或 warning，不能再以 `isLoading + 空列表` 猜测状态。

建议命令结果：

```kotlin
sealed interface BookshelfCommandResult {
    data class Success(val changedBookIds: Set<String> = emptySet()) : BookshelfCommandResult
    data class Partial(
        val changedBookIds: Set<String>,
        val failed: Map<String, BookshelfError>,
    ) : BookshelfCommandResult
    data class Failure(val error: BookshelfError) : BookshelfCommandResult
}

sealed interface BookshelfError {
    val diagnostic: String?

    data class Retryable(override val diagnostic: String? = null) : BookshelfError
    data class InvalidRequest(override val diagnostic: String? = null) : BookshelfError
    data class NotFound(override val diagnostic: String? = null) : BookshelfError
    data class PermissionDenied(override val diagnostic: String? = null) : BookshelfError
    data class Conflict(override val diagnostic: String? = null) : BookshelfError
    data class Unexpected(override val diagnostic: String? = null) : BookshelfError
}
```

UI 映射固定如下：

| API 结果 | UI 映射 | 重试 |
|---|---|---|
| `Loading` 且无旧 snapshot | 全页 loading/skeleton | 否 |
| `Data(snapshot)` 且无书 | empty；仍显示可用入口 | 否 |
| `Data(snapshot, warnings)` | content + 非阻断提示 | 视 warning 而定 |
| `Failure(error, previous = null)` | 全页 error | 仅 `Retryable` 显示重试 |
| `Failure(error, previous != null)` | 保留 content + snackbar/banner | 仅 `Retryable` 显示重试 |
| `CommandResult.Success` | 不直接改列表；等待 SSOT 回流，可提示成功 | 否 |
| `CommandResult.Partial` | 保留 SSOT 内容，明确成功/失败数量和失败项 | 失败项可按错误类型重试 |
| `CommandResult.Failure` | 保留 SSOT 内容并提示；排序预览回退到 snapshot | `Retryable` 可重试 |

API 层不提供本地化文案；UI 根据错误类型选资源，`diagnostic` 仅用于日志/兜底。底层异常不得直接成为跨模块稳定契约。

## 6. 写命令与 effect 的唯一所有者

| 事实/动作 | 唯一业务所有者 | 唯一 effect 收集层 |
|---|---|---|
| 书籍删除、原文件删除协调、删除回调 | `bookshelf:api` | `BookshelfRouteScreen` 展示结果；列表只从 SSOT 更新 |
| 书籍分组变更、分组 CRUD/排序 | `bookshelf:api` | Route 只展示结果/关闭 sheet |
| 书籍手动顺序 | `bookshelf:api` | Route 展示失败；Screen 可保留拖拽预览但不持久化 |
| 阅读进度保存/恢复/同步 | `reader:api`（建立前为旧 reader 路径） | reader Route/兼容 host |
| 阅读进度书架投影 | `bookshelf:api` 只读查询 | 无 effect |
| 本地/远程/URL/JSON 导入 | `catalog:api`（建立前为 app 兼容路径） | app/catalog Route 处理导航与 Activity Result |
| 目录更新与缓存获取 | catalog 内容获取边界（建立前复用既有 UseCase） | Route 仅提示；队列状态来自唯一业务流 |
| 书架/主题/导出偏好 | 对应 settings gateway / settings feature | 纯设置成功通常无 effect；失败由发起 Route 收集 |
| 书架导出/上传 | `backup-sync:api`（建立前为 app 兼容路径） | Route 处理文件 launcher、复制与提示 |
| 打开 reader、详情、全局搜索、导入/管理页 | app shell 根导航 | `BookshelfRouteScreen` 收集后调用 app 提供的 callback |

目标实现中 `BookshelfRouteScreen` 是 `BookshelfEffect` 的唯一收集者。Snackbar host 可由 Route 创建并传给纯 Screen；Screen 不再同时订阅 effect。文件、剪贴板、外部 Intent 和根导航均在 Route/app shell 处理。当前 `pendingUploadUrl` 状态在迁移时改为一次性 effect，避免与 `_effects` 形成双通道。

## 7. 债务分类与处理决定

| 证据 | 分类 | 本批决定 | 删除/完成条件 |
|---|---|---|---|
| Repository/UseCase 反向依赖 `BookShelfItem`/`BookUiItem` | 边界阻断 | Phase 0 记录，M2 正式 impl 前修复 | data/domain 不再导入 `ui.main.bookshelf` |
| 主书架与管理页各自实现排序 | 等价简化 | M2 合并为一个有契约测试的排序规则 | 两页同输入/设置得到同顺序；旧实现删除 |
| 拖拽排序先改可变 `Book.order`，失败无明确回滚模型 | 边界阻断/明确风险 | M2 以 ID 顺序命令 + SSOT 回流修正 | 失败测试证明 UI 回退且数据库无部分顺序 |
| 删除逐书执行文件/回调/章节操作，异常时不能表达部分完成 | 明确风险 | 现状成功语义先由测试固定；M2 在 API 适配器中增加明确 partial/error 映射 | 正常、缺失、失败、部分完成、重复点击测试通过 |
| Room Flow 与 `UP_BOOKSHELF` EventBus 同时存在 | 全局兼容债 | 本批保留；EventBus 只作通知，不作新 UI SSOT | 所有消费者改用业务 Flow 后单独删除 |
| `CacheBook`/Service/SourceCallBack 由 ViewModel 直接协调 | 与首批核心写命令无关的系统重构 | 保留在 app 兼容边界，禁止搬进 feature UI | catalog/缓存边界建立并保持 Service 协议 |
| `AppConfig`、`LocalConfig`、`BookshelfManageScreenConfig` 直接出现在 UI/VM | 边界阻断 | 由设置快照/命令接缝替代 | 新 feature 不导入这些全局配置 |
| Activity Result、Service 启动和导出路径逻辑位于 Screen | UI 边界问题 | 新 UI 迁到 Route；旧 UI 本批不动 | Screen 只接收 state/intent/callback |
| 初始加载用 500 ms fallback，查询失败没有显式 error | 明确缺陷风险 | M2 使用 `BookshelfQueryState` 修复；不把失败当空态 | loading/empty/retryable/fatal/previous-content 测试通过 |
| 导入 UseCase 直接 toast 且混合网络、文件、搜索和写库 | catalog 边界阻断 | 不纳入首批 bookshelf impl，留给 catalog 迁移卡 | catalog API 有进度/部分结果且不依赖 UI/Context toast |
| `AppShellSettingsGateway` 组合值未使用 | 等价净删除 | 迁移触及该 VM 时删除 | 无行为变化且编译/测试通过 |

## 8. 验证与回滚

自动验证基线：

- `verifyConfigArchitecture`：现有偏好、ViewModel DAO 和非 ViewModel UI DAO 双向棘轮；新增代码不得加入 legacy baseline。
- `DeleteBooksUseCaseTest`：空输入、仅删除可解析目标、本地/网络回调与最终 SSOT 删除顺序。
- `UpdateBooksGroupUseCaseTest`：空输入、跳过未变化项、只写变化后的分组分配。
- `MainNavigatorBookshelfTest`：导入/管理路由的 Home 返回栈与 route 序列化。
- 既有 `BookShelfIntroQueryTest`、`BookGroupMutationRepositoryTest`、`BookshelfSettingsMappingTest`、`ReadBookDomainSplitBoundaryTest` 继续通过。
- CI 的 `testAppDebugUnitTest + lintAppDebug + verifyConfigArchitecture + assembleAppDebug` 保持必经，不新增 legacy baseline。

仍需在 Phase 2 入口切换前补齐的测试：查询正常/空/失败/重试、删除 partial/retry/重复点击、排序失败回流、reader 写进度后书架投影刷新，以及设备上的大字体、TalkBack、多窗口和进程恢复。

Phase 0 的回滚仅需移除本卡和新增测试；没有 Room、Manifest、资源、Service、备份格式或生产调用链需要回滚。

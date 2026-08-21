# 书架 Phase 7 迁移卡：正式实现垂直切片

> 状态：正式实现已建立并完成绑定切换；旧书架 UI 与灰度开关不变，仍为 `experiment`。
> 日期：2026-08-22
> 上游计划：[`compose-ui-module-migration-plan.md`](./compose-ui-module-migration-plan.md) 第 5 节 Phase 7
> 前置迁移卡：[`bookshelf-phase2-migration-card.md`](./bookshelf-phase2-migration-card.md)

## 1. 范围

只把书架查询、分组、删除和排序四组 API 的实现从 `:app` 移到 `:feature:bookshelf:impl`。
不改数据库 schema、备份格式、Intent、Service 协议、书源规则语义，不切换默认 UI，
不搬迁 `BookRepository`、图书详情、阅读器或规则引擎代码。

## 2. 依赖形态

```text
:app ─────────────────────→ :feature:bookshelf:ui
  └── 仅总装配 ───────────→ :feature:bookshelf:impl

:feature:bookshelf:ui ────→ :feature:bookshelf:api
:feature:bookshelf:impl ──→ :feature:bookshelf:api + :core:database
```

`:feature:bookshelf:impl` 不依赖 `:app`，不含 Compose、Activity、Fragment 或根导航代码。

## 3. 迁移的实现

| 旧位置（`:app`） | 新位置 | 说明 |
|---|---|---|
| `feature/bookshelf/compat/LegacyBookshelfAdapter.kt` | `DefaultBookshelfRepository.kt` | API 映射、错误分类、命令校验、SSOT 组合 |
| `data/repository/BookshelfRepository.sortBooks` | `BookshelfMapping.sortedForShelf` | 排序语义；见第 5 节等价简化 |
| `domain/usecase/UpdateBooksGroupUseCase`（书架路径） | `RoomBookshelfStore.moveToGroup` | 仅书架分组写入 |
| `domain/usecase/ReorderBooksUseCase` | `DefaultBookshelfRepository.reorderBooks` + `RoomBookshelfStore.applyBookOrders` | order 列语义不变 |
| `domain/usecase/DeleteBooksUseCase`（书架路径） | `DefaultBookshelfRepository.deleteBooks` + `RoomBookshelfStore` | 副作用顺序不变 |
| `domain/usecase/ReorderBookGroupsUseCase` | `DefaultBookshelfRepository.reorderGroups` | 校验语义不变 |
| `data/repository/BookGroupMutationRepository`（书架路径） | `RoomBookshelfStore` 的分组增删改 | 书架 API 从不传 tag 规则，故不复制规则重放分支 |

旧的 `:app` UseCase / Repository 保持原样，继续服务旧书架 UI 与其它调用方；它们不再是书架 API 的实现。

## 4. 保留在 `:app` 的宿主接缝

`:feature:bookshelf:impl` 声明两个宿主契约，由 `:app` 在 `di/BookshelfHostAdapters.kt` 实现并注入：

| 契约 | 由谁承接 | 为什么不在 impl |
|---|---|---|
| `BookshelfPreferencesHost` | `BookshelfSettingsGateway` + `BookshelfDeleteOriginalGateway` | 偏好存储仍在 app shell，`:core:preferences` 未建立 |
| `BookshelfBookRemovalHost` | `LocalBookGateway` + `BookSourceCallbackGateway` | 本地文件删除与书源回调需要文件系统和规则引擎 |

宿主适配器只做转发，不含书架业务规则。删除条件：`:core:preferences` 建立后
`BookshelfPreferencesHost` 下沉；本地书籍与书源回调接缝独立立项后 `BookshelfBookRemovalHost` 下沉。

## 5. 业务逻辑分类

| 项 | 分类 | 证据与验证 |
|---|---|---|
| 查询/命令/错误映射 | 保持 | `BookshelfImplContractTest` 复用旧适配器测试的断言 |
| 阅读进度只读投影 | 保持 | 进度仍由阅读器写入，书架只投影 |
| 排序的 per-group 覆盖参数 | 等价简化 | 旧 `sortBooks(group=null)` 从未传分组，分支恒不触发；已删除该死参数 |
| DAO 调用移出主线程 | 边界阻断修复 | 旧 `BookDomainRepositoryImpl` 直接在调用线程执行阻塞 DAO；新实现统一 `Dispatchers.IO` |
| `Book.isLocal` 判定 | 等价简化 | 判定下沉为 `Book.isLocalBook()`，`help/book/BookExtensions.isLocal` 改为委托，规则单一所有者 |
| 分组增删改的 tag 规则重放 | 移除（不适用） | 书架 API 的 `createGroup`/`updateGroup` 恒传 `pattern = null` / `ruleToSave = null`，规则分支不可达 |
| 删除失败的 partial 语义 | 保持 | 副作用逐本执行、章节先删、整批删书在后，失败后按剩余书目分类 |

未列出的行为默认保持兼容。本次没有"有意改变产品行为"项。

## 6. 单一写实现

`bookshelfImplModule` 是书架四个 API 接口的唯一 Koin 绑定；`:app` 只加载该 module。
架构护栏禁止 `:app` 导入 `BookshelfQuery` / `BookshelfCommands` / `BookshelfGroupCommands` /
`BookshelfPreferencesGateway`，也禁止在 `app/src/main/java/io/legado/app/feature/` 下重建适配器。
新旧 UI 共用同一个 Room SSOT，不存在第二个书架数据所有者，也没有 optimistic 双写。

## 7. 验证

- `:feature:bookshelf:impl:testDebugUnitTest`（API 契约：正常/空/失败/重试/重复命令/partial failure/排序完整性/只读进度投影）
- `:feature:bookshelf:api:testDebugUnitTest`、`:feature:bookshelf:ui:testDebugUnitTest`
- `:feature:bookshelf:impl:lintDebug`、`:feature:bookshelf:ui:lintDebug`
- `verifyConfigArchitecture`（含 `verifyArchitectureGuardFixture`、`verifyFeatureMigrationGovernance`）与各模块 `verifyModuleDependencies`
- `:app:compileAppDebugKotlin`、`:app:assembleAppDebug`、`:app:assembleAppRelease`

实际执行结果记录在 [`phase7-9-migration-record.md`](./phase7-9-migration-record.md)。

## 8. 回滚

只需把 `bookshelfImplModule` 换回等价的 app 绑定即可回退实现；数据库、持久化格式和 SSOT 不变，
同一构建中始终只有一个写实现。关闭 `bookshelfFeatureEnabled` 仍回到旧书架 UI，
且不需要恢复 app adapter——旧 UI 不消费书架 API。

## 9. 删除条件

- 已删除：`LegacyBookshelfAdapter` 及其 Koin 绑定、`LegacyBookshelfAdapterMappingTest`、
  架构护栏中的书架兼容适配器白名单。
- 待删除（Phase 8.1 UI 转正后）：旧书架 UI `ui/main/bookshelf/BookshelfScreen.kt`、
  `USE_COMPOSE_BOOKSHELF_FEATURE` 开关、`bookshelfFeatureEnabled` Gradle property。
  阻塞条件：Release/R8 可安装产物、手机/横屏分屏/大字体/TalkBack 设备矩阵、进程恢复验收，
  以及一个稳定版本周期的默认观察，均需人工签收，本次未发生。
- 待删除（`:core:preferences` 建立后）：`AppBookshelfPreferencesHost`。
- 待删除（本地书籍/书源回调接缝下沉后）：`AppBookshelfBookRemovalHost`。

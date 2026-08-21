# bookshelf 实现切换证据（Phase 7）

> 日期：2026-08-22
> 迁移卡：[`bookshelf-phase7-migration-card.md`](../bookshelf-phase7-migration-card.md)
> 登记项：`bookshelf.implementationStatus = formal_impl`

## 切换内容

`bookshelf` 的 `BookshelfQuery`、`BookshelfPreferencesGateway`、`BookshelfCommands`、`BookshelfGroupCommands` 由 `:app` 兼容适配器改为 `:feature:bookshelf:impl` 提供。

| 旧位置 | 新位置 |
|---|---|
| `LegacyBookshelfAdapter` 的映射/错误分类/命令校验/SSOT 组合 | `DefaultBookshelfRepository` |
| `BookshelfRepository.sortBooks` | `BookshelfMapping.sortedForShelf` |
| `UpdateBooksGroupUseCase` / `ReorderBooksUseCase` / `DeleteBooksUseCase` / `ReorderBookGroupsUseCase` 的书架路径 | `DefaultBookshelfRepository` + `RoomBookshelfStore` |
| `BookGroupMutationRepository` 的分组增删改（书架路径） | `RoomBookshelfStore` |

Room 读写收敛到 `RoomBookshelfStore`，业务规则位于 `DefaultBookshelfRepository`。

## 唯一绑定

- `bookshelfImplModule` 是上述 API 在运行时的唯一 Koin 绑定，由 `App.onCreate()` 加载。
- `appModule.kt` 中原有的逐接口绑定已删除。
- 架构护栏 `formalImplBoundApis` 禁止 `:app` 主源码导入这些接口，重新绑定会导致
  `verifyConfigArchitecture` 失败。
- 模块护栏 `verifyModuleDependencies` 保证 `:feature:bookshelf:impl` 不依赖 `:app`，
  并要求它提供对应 API 依赖、唯一 Koin module 和 `*ContractTest.kt`。

## 数据与 SSOT

数据库 schema、表、字段、索引与备份序列化契约均未改变。写入后仍由同一 Room Flow 回流到 UI，
没有 optimistic 双写，切换前后的 SSOT 是同一个。

判定一致性：`Book.isLocal` 的两分支判定（`type == 0` 时回落到 `origin`）已下沉为
`Book.isLocalBook()`（`:core:database`），`help/book/BookExtensions.isLocal` 改为委托。
删除本地书时的分支与旧 `DeleteBooksUseCase` 完全一致，不存在只按位掩码判断而漏删原文件的路径。

## 保留的宿主接缝

| 接缝 | app 实现 | 删除条件 |
|---|---|---|
| `BookshelfPreferencesHost` | `di/BookshelfHostAdapters.kt` | `:core:preferences` 建立后下沉 |
| `BookshelfBookRemovalHost` | `di/BookshelfHostAdapters.kt` | 本地书籍/书源回调接缝独立立项后下沉 |

宿主适配器只做转发，不含 feature 业务规则。

## 验证

- `:feature:bookshelf:impl:testDebugUnitTest`（21 例）
- `verifyMigrationGovernance`（架构护栏 + 护栏夹具 + 迁移登记表 + 全模块依赖检查）
- `:app:compileAppDebugKotlin`、`:app:assembleAppDebug`、`:app:assembleAppRelease`

完整结果见 [`phase7-9-migration-record.md`](../phase7-9-migration-record.md)。

## 回滚

把 `bookshelfImplModule` 换回等价的 app 绑定即可回退实现。数据库与持久化格式不变，
同一构建中始终只有一个写实现。UI 灰度开关与本切换无关。

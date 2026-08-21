# catalog 实现切换证据（Phase 8.4）

> 日期：2026-08-22
> 迁移卡：[`catalog-phase8-migration-card.md`](../catalog-phase8-migration-card.md)
> 登记项：`catalog.implementationStatus = formal_impl`

## 切换内容

`catalog` 的 `CatalogQuery`、`CatalogCommands` 由 `:app` 兼容适配器改为 `:feature:catalog:impl` 提供。

| 旧位置 | 新位置 |
|---|---|
| `LegacyCatalogAdapter` 的查询组合、摘要映射、命令错误封装 | `DefaultCatalogRepository` |
| `ExploreRepositoryImpl.getExploreGroups` / `getExploreSources` 的发现页路径 | `RoomCatalogSourceStore` |
| `ExploreRepositoryImpl.topSource` 的发现页路径 | `RoomCatalogSourceStore.pinSource` |

Room 读写收敛到 `RoomCatalogSourceStore`，业务规则位于 `DefaultCatalogRepository`。

## 唯一绑定

- `catalogImplModule` 是上述 API 在运行时的唯一 Koin 绑定，由 `App.onCreate()` 加载。
- `appModule.kt` 中原有的逐接口绑定已删除。
- 架构护栏 `formalImplBoundApis` 禁止 `:app` 主源码导入这些接口，重新绑定会导致
  `verifyConfigArchitecture` 失败。
- 模块护栏 `verifyModuleDependencies` 保证 `:feature:catalog:impl` 不依赖 `:app`，
  并要求它提供对应 API 依赖、唯一 Koin module 和 `*ContractTest.kt`。

## 数据与 SSOT

数据库 schema、表、字段、索引与备份序列化契约均未改变。写入后仍由同一 Room Flow 回流到 UI，
没有 optimistic 双写，切换前后的 SSOT 是同一个。

等价简化：旧适配器的 `pinSource` 先 `getExploreSources("", "").first()` 拉全量再线性查找，
现在按主键取 `BookSourcePart`；查询条件与 `minOrder - 1` 的写入语义不变。

## 保留的宿主接缝

| 接缝 | app 实现 | 删除条件 |
|---|---|---|
| `CatalogSourceRemovalHost` | `di/CatalogHostAdapters.kt` | 源管理 feature 化后由对应 impl 承接 |

宿主适配器只做转发，不含 feature 业务规则。

## 验证

- `:feature:catalog:impl:testDebugUnitTest`（9 例）
- `verifyMigrationGovernance`（架构护栏 + 护栏夹具 + 迁移登记表 + 全模块依赖检查）
- `:app:compileAppDebugKotlin`、`:app:assembleAppDebug`、`:app:assembleAppRelease`

完整结果见 [`phase7-9-migration-record.md`](../phase7-9-migration-record.md)。

## 回滚

把 `catalogImplModule` 换回等价的 app 绑定即可回退实现。数据库与持久化格式不变，
同一构建中始终只有一个写实现。UI 灰度开关与本切换无关。

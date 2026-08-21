# Catalog Phase 8.4 迁移卡：正式实现

> 状态：正式实现已建立并完成绑定切换；旧发现 UI 与灰度开关不变，仍为 `experiment`。
> 日期：2026-08-22
> 上游计划：[`compose-ui-module-migration-plan.md`](./compose-ui-module-migration-plan.md) 第 5 节 Phase 8
> 前置迁移卡：[`catalog-phase3-migration-card.md`](./catalog-phase3-migration-card.md)

## 1. 范围

把发现页的书源列表查询、置顶和删除三个 API 的实现从 `:app` 移到 `:feature:catalog:impl`。
不改书源表结构、规则语义、导入流程、搜索与书籍详情；不搬迁 `ExploreRepository` 的
探索抓取（`WebBook`）、书架投影与搜索结果持久化。

## 2. 依赖形态

```text
:app ─────────────────────→ :feature:catalog:ui
  └── 仅总装配 ───────────→ :feature:catalog:impl

:feature:catalog:ui ──────→ :feature:catalog:api
:feature:catalog:impl ────→ :feature:catalog:api + :core:database
```

## 3. 迁移的实现

| 旧位置（`:app`） | 新位置 | 说明 |
|---|---|---|
| `feature/catalog/compat/LegacyCatalogAdapter.kt` | `DefaultCatalogRepository.kt` | 查询组合、摘要映射、命令错误封装 |
| `ExploreRepositoryImpl.getExploreGroups/getExploreSources`（发现页路径） | `RoomCatalogSourceStore` | `group:` 前缀语法与分组筛选不变 |
| `ExploreRepositoryImpl.topSource`（发现页路径） | `RoomCatalogSourceStore.pinSource` | `minOrder - 1` 语义不变 |

`ExploreRepository` 的其余能力（书架投影、探索抓取、搜索结果写入）仍留在 `:app`，
服务旧发现页与其它调用方。

等价简化：旧适配器的 `pinSource` 先 `getExploreSources("", "").first()` 拉全量再线性查找，
现在直接按主键取 `BookSourcePart`；查询条件与写入语义不变。

## 4. 保留在 `:app` 的宿主接缝

| 契约 | 由谁承接 | 为什么不在 impl |
|---|---|---|
| `CatalogSourceRemovalHost` | `SourceHelp.deleteBookSource` | 删除同时要清运行时源变量缓存和 `SourceConfig` 条目 |

## 5. 业务逻辑分类

| 项 | 分类 | 证据与验证 |
|---|---|---|
| 查询/分组/`group:` 语法/失败态 | 保持 | `CatalogImplContractTest` 覆盖 loading、空、失败、分组前缀 |
| `exploreEnabled` 投影 | 保持 | 契约测试覆盖 `enabledExplore && hasExploreUrl` 三种组合 |
| 置顶 | 保持 | 契约测试断言 order |
| 删除 | 保持 | 仍走 `SourceHelp.deleteBookSource`，impl 不复制删除逻辑 |
| 置顶前的全量拉取 | 等价简化 | 改为主键查询，见第 3 节 |
| DAO 调用移出主线程 | 边界阻断修复 | `RoomCatalogSourceStore` 统一 `Dispatchers.IO` |

本次没有"有意改变产品行为"项。

## 6. 单一写实现

`catalogImplModule` 是 `CatalogQuery` / `CatalogCommands` 的唯一 Koin 绑定。架构护栏禁止
`:app` 导入这两个接口，也禁止在 `app/src/main/java/io/legado/app/feature/` 下重建适配器。

## 7. 验证

见 [`phase7-9-migration-record.md`](./phase7-9-migration-record.md)。

## 8. 回滚

只需把 `catalogImplModule` 换回等价的 app 绑定；书源表和 SSOT 不变。
关闭 `catalogFeatureEnabled` 仍回到旧发现页，且不需要恢复 app adapter。

## 9. 删除条件

- 已删除：`LegacyCatalogAdapter`、对应 Koin 绑定、架构护栏中的 catalog 兼容适配器白名单。
- 待删除（UI 转正后）：旧发现页 `ui/main/explore/ExploreScreen.kt`、
  `USE_COMPOSE_CATALOG_FEATURE`、`catalogFeatureEnabled`。阻塞条件：规则语义、导入兼容、
  详情/发现恢复的设备验收、Release/R8 产物与一个稳定版本周期的默认观察，均需人工签收，
  本次未发生。
- 待删除（源管理 feature 化后）：`AppCatalogSourceRemovalHost`。

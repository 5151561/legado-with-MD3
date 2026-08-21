# RSS Phase 8.3 迁移卡：正式实现

> 状态：正式实现已建立并完成绑定切换；旧 RSS UI 与灰度开关不变，仍为 `experiment`。
> 日期：2026-08-22
> 上游计划：[`compose-ui-module-migration-plan.md`](./compose-ui-module-migration-plan.md) 第 5 节 Phase 8
> 前置迁移卡：[`rss-phase3-migration-card.md`](./rss-phase3-migration-card.md)

## 1. 范围

把 RSS 源列表查询、打开目标解析、置顶、停用和删除五个 API 的实现从 `:app` 移到
`:feature:rss:impl`。不改 RSS 表结构、订阅规则语义、WebView 安全模型，不搬迁文章、收藏、
阅读记录仓库，不切换默认 UI。

## 2. 依赖形态

```text
:app ─────────────────→ :feature:rss:ui
  └── 仅总装配 ───────→ :feature:rss:impl

:feature:rss:ui ──────→ :feature:rss:api
:feature:rss:impl ────→ :feature:rss:api + :core:database
```

## 3. 迁移的实现

| 旧位置（`:app`） | 新位置 | 说明 |
|---|---|---|
| `feature/rss/compat/LegacyRssAdapter.kt` | `DefaultRssRepository.kt` | 查询组合、摘要映射、命令错误封装 |
| `domain/usecase/ResolveRssOpenTargetUseCase.kt` | `DefaultRssRepository.resolveOpenTarget` | 单 URL / `sortUrl` / `::` 分隔 / 外链判定 |
| `data/repository/RssRepository.topSources`（RSS 首页路径） | `RoomRssSourceStore.pinSource` | `minOrder - 1` 语义不变 |
| `data/repository/RssRepository.disableSource`（RSS 首页路径） | `RoomRssSourceStore.setEnabled` | 语义不变 |

`ResolveRssOpenTargetUseCase` 只有旧适配器一个调用方，已随适配器一起删除，打开目标解析
因此仍是单一所有者。`RssRepository` 的其余方法继续服务源管理、导入等旧调用方。

## 4. 保留在 `:app` 的宿主接缝

| 契约 | 由谁承接 | 为什么不在 impl |
|---|---|---|
| `RssSourceScriptHost` | `runScriptWithContext { source.evalJS(...) }` | 规则引擎在 app / `:modules:rhino`，`:core:rule-engine` 未建立 |
| `RssSourceRemovalHost` | `RssRepository.deleteByIds` → `SourceHelp.deleteRssSources` | 删除同时要清源变量运行时缓存，且需与源管理保持同一事务与唯一所有者 |

删除条件：`:core:rule-engine` 建立后 `RssSourceScriptHost` 下沉；源删除接缝随源管理
feature 化后由对应 impl 承接。

## 5. 业务逻辑分类

| 项 | 分类 | 证据与验证 |
|---|---|---|
| 查询/摘要映射/失败态 | 保持 | `RssImplContractTest` 覆盖 loading、空、失败、空图标投影 |
| 打开目标解析（含 `<js>` / `@js:` / `::`） | 保持 | 契约测试逐分支覆盖，JS 求值交由宿主并断言脚本切片 |
| 置顶 / 停用 | 保持 | 契约测试断言 order 与幂等停用 |
| 删除 | 保持 | 仍走 `SourceHelp.deleteRssSources` 的事务与缓存清理，impl 不复制删除逻辑 |
| DAO 调用移出主线程 | 边界阻断修复 | `RoomRssSourceStore` 统一 `Dispatchers.IO` |

本次没有"有意改变产品行为"项。

## 6. 单一写实现

`rssImplModule` 是 `RssQuery` / `RssCommands` 的唯一 Koin 绑定。架构护栏禁止 `:app`
导入这两个接口，也禁止在 `app/src/main/java/io/legado/app/feature/` 下重建 RSS 适配器。

## 7. 验证

见 [`phase7-9-migration-record.md`](./phase7-9-migration-record.md)。

## 8. 回滚

只需把 `rssImplModule` 换回等价的 app 绑定；Room 表、订阅数据和 SSOT 不变。
关闭 `rssFeatureEnabled` 仍回到旧 RSS UI，且不需要恢复 app adapter。

## 9. 删除条件

- 已删除：`LegacyRssAdapter`、`ResolveRssOpenTargetUseCase`、对应 Koin 绑定、
  架构护栏中的 RSS 兼容适配器白名单。
- 待删除（UI 转正后）：旧 RSS UI `ui/main/rss/RssScreen.kt`、`USE_COMPOSE_RSS_FEATURE`、
  `rssFeatureEnabled`。阻塞条件：JS 单 URL / WebView 外链 / 返回栈的设备验收、Release/R8
  产物与一个稳定版本周期的默认观察，均需人工签收，本次未发生。
- 待删除（`:core:rule-engine` 建立后）：`AppRssSourceScriptHost`。

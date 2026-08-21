# rss 实现切换证据（Phase 8.3）

> 日期：2026-08-22
> 迁移卡：[`rss-phase8-migration-card.md`](../rss-phase8-migration-card.md)
> 登记项：`rss.implementationStatus = formal_impl`

## 切换内容

`rss` 的 `RssQuery`、`RssCommands` 由 `:app` 兼容适配器改为 `:feature:rss:impl` 提供。

| 旧位置 | 新位置 |
|---|---|
| `LegacyRssAdapter` 的查询组合、摘要映射、命令错误封装 | `DefaultRssRepository` |
| `ResolveRssOpenTargetUseCase`（单 URL / `sortUrl` / `::` / 外链判定） | `DefaultRssRepository.resolveOpenTarget` |
| `RssRepository.topSources` / `disableSource` 的 RSS 首页路径 | `RoomRssSourceStore` |

Room 读写收敛到 `RoomRssSourceStore`，业务规则位于 `DefaultRssRepository`。

## 唯一绑定

- `rssImplModule` 是上述 API 在运行时的唯一 Koin 绑定，由 `App.onCreate()` 加载。
- `appModule.kt` 中原有的逐接口绑定已删除。
- 架构护栏 `formalImplBoundApis` 禁止 `:app` 主源码导入这些接口，重新绑定会导致
  `verifyConfigArchitecture` 失败。
- 模块护栏 `verifyModuleDependencies` 保证 `:feature:rss:impl` 不依赖 `:app`，
  并要求它提供对应 API 依赖、唯一 Koin module 和 `*ContractTest.kt`。

## 数据与 SSOT

数据库 schema、表、字段、索引与备份序列化契约均未改变。写入后仍由同一 Room Flow 回流到 UI，
没有 optimistic 双写，切换前后的 SSOT 是同一个。

删除路径未复制：`deleteSource` 仍经 `RssRepository.deleteByIds` → `SourceHelp.deleteRssSources`，
保留原有事务与源变量缓存清理，impl 不持有第二份删除逻辑。

## 保留的宿主接缝

| 接缝 | app 实现 | 删除条件 |
|---|---|---|
| `RssSourceScriptHost` | `di/RssHostAdapters.kt` | `:core:rule-engine` 建立后下沉 |
| `RssSourceRemovalHost` | `di/RssHostAdapters.kt` | 源管理 feature 化后由对应 impl 承接 |

宿主适配器只做转发，不含 feature 业务规则。

## 验证

- `:feature:rss:impl:testDebugUnitTest`（16 例）
- `verifyMigrationGovernance`（架构护栏 + 护栏夹具 + 迁移登记表 + 全模块依赖检查）
- `:app:compileAppDebugKotlin`、`:app:assembleAppDebug`、`:app:assembleAppRelease`

完整结果见 [`phase7-9-migration-record.md`](../phase7-9-migration-record.md)。

## 回滚

把 `rssImplModule` 换回等价的 app 绑定即可回退实现。数据库与持久化格式不变，
同一构建中始终只有一个写实现。UI 灰度开关与本切换无关。

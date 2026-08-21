# ai 实现切换证据（Phase 8.5）

> 日期：2026-08-22
> 迁移卡：[`ai-phase8-migration-card.md`](../ai-phase8-migration-card.md)
> 登记项：`ai.implementationStatus = formal_impl`

## 切换内容

`ai` 的 `AiOverviewQuery`、`AiCommands` 由 `:app` 兼容适配器改为 `:feature:ai:impl` 提供。

| 旧位置 | 新位置 |
|---|---|
| `LegacyAiAdapter` 的三流组合、概览映射、默认模型投影、命令错误封装 | `DefaultAiRepository` |
| `AiProfileRepository.observeProviders/Models/Presets` 的概览路径 | `RoomAiProfileStore` |
| `AiTaskType`（`ai_task_presets.taskType` 的持久化词表） | `:core:database` 的同名包，FQN 不变 |

Room 读写收敛到 `RoomAiProfileStore`，业务规则位于 `DefaultAiRepository`。

## 唯一绑定

- `aiImplModule` 是上述 API 在运行时的唯一 Koin 绑定，由 `App.onCreate()` 加载。
- `appModule.kt` 中原有的逐接口绑定已删除。
- 架构护栏 `formalImplBoundApis` 禁止 `:app` 主源码导入这些接口，重新绑定会导致
  `verifyConfigArchitecture` 失败。
- 模块护栏 `verifyModuleDependencies` 保证 `:feature:ai:impl` 不依赖 `:app`，
  并要求它提供对应 API 依赖、唯一 Koin module 和 `*ContractTest.kt`。

## 数据与 SSOT

数据库 schema、表、字段、索引与备份序列化契约均未改变。写入后仍由同一 Room Flow 回流到 UI，
没有 optimistic 双写，切换前后的 SSOT 是同一个。

写入未复制：`setDefaultModel` 会按 app 侧的 `AiGenerationParams` / `AiTaskRuntimeOptions`
重写默认预设，仍由 `AiProfileGateway` 唯一执行；impl 只转发并分类错误，不产生第二个预设写入者。
概览投影不含 `apiKey`。

## 保留的宿主接缝

| 接缝 | app 实现 | 删除条件 |
|---|---|---|
| `AiDefaultModelHost` | `di/AiHostAdapters.kt` | AI 领域模型（`AiGenerationParams` 等）与 JSON 接缝下沉 core 后，`setDefaultModel` 迁入 impl |

宿主适配器只做转发，不含 feature 业务规则。

## 验证

- `:feature:ai:impl:testDebugUnitTest`（8 例）
- `verifyMigrationGovernance`（架构护栏 + 护栏夹具 + 迁移登记表 + 全模块依赖检查）
- `:app:compileAppDebugKotlin`、`:app:assembleAppDebug`、`:app:assembleAppRelease`

完整结果见 [`phase7-9-migration-record.md`](../phase7-9-migration-record.md)。

## 回滚

把 `aiImplModule` 换回等价的 app 绑定即可回退实现。数据库与持久化格式不变，
同一构建中始终只有一个写实现。UI 灰度开关与本切换无关。

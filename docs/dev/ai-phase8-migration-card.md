# AI Phase 8.5 迁移卡：正式实现

> 状态：概览查询的正式实现已建立并完成绑定切换；默认模型写入仍由 `:app` 唯一承接。
> 旧 AI 配置 UI 与灰度开关不变，仍为 `experiment`。
> 日期：2026-08-22
> 上游计划：[`compose-ui-module-migration-plan.md`](./compose-ui-module-migration-plan.md) 第 5 节 Phase 8
> 前置迁移卡：[`ai-phase3-migration-card.md`](./ai-phase3-migration-card.md)

## 1. 范围

把 AI 概览（供应商、模型、预设计数、默认模型投影）的实现从 `:app` 移到 `:feature:ai:impl`。
不改 AI 表结构、生成协议、密钥存储、会话与流式生成；不搬迁 `AiProfileRepository` 的写入能力。

## 2. 依赖形态

```text
:app ────────────────→ :feature:ai:ui
  └── 仅总装配 ──────→ :feature:ai:impl

:feature:ai:ui ──────→ :feature:ai:api
:feature:ai:impl ────→ :feature:ai:api + :core:database
```

## 3. 迁移的实现

| 旧位置（`:app`） | 新位置 | 说明 |
|---|---|---|
| `feature/ai/compat/LegacyAiAdapter.kt` | `DefaultAiRepository.kt` | 三条 SSOT 流的组合、概览映射、默认模型投影、命令错误封装 |
| `AiProfileRepository.observeProviders/Models/Presets`（概览路径） | `RoomAiProfileStore` | 直接消费 `AiProfileDao` 的 Room 流 |

`AiTaskType` 从 `:app` 的 `domain/model/AiModels.kt` 移入 `:core:database` 的同名包。
它是 `ai_task_presets.taskType` 的持久化词表，与 schema 同住后由 `:app` 与 AI 实现共用一份常量，
包名与 FQN 不变，`:app` 的既有引用无需改动。

## 4. 保留在 `:app` 的宿主接缝

| 契约 | 由谁承接 | 为什么不在 impl |
|---|---|---|
| `AiDefaultModelHost` | `AiProfileGateway.setDefaultModel` | 该写入会按 app 侧的 `AiGenerationParams` / `AiTaskRuntimeOptions` 重写默认预设；把它复制进 impl 会产生第二个预设写入者 |

删除条件：AI 领域模型（`AiGenerationParams`、`AiTaskRuntimeOptions`、`AiPromptTemplate`、
`TranslationConstants`）与 JSON 序列化接缝下沉到 core 模块后，`setDefaultModel` 迁入
`:feature:ai:impl`，`AppAiDefaultModelHost` 随之删除。这是独立工作项，不在本迁移卡范围内。

## 5. 业务逻辑分类

| 项 | 分类 | 证据与验证 |
|---|---|---|
| 概览查询与失败态 | 保持 | `AiImplContractTest` 覆盖 loading、未配置、失败 |
| 模型可用性级联（供应商停用即不可用） | 保持 | 契约测试覆盖启用/停用/未知供应商三种组合 |
| 默认模型投影（默认翻译预设） | 保持 | 契约测试覆盖非默认预设与其它任务类型不参与判定 |
| 默认模型写入 | 保持 | 仍由 `AiProfileGateway` 唯一执行，impl 只转发并分类错误 |

本次没有"有意改变产品行为"项。密钥仍只存在于 `:app` 的 provider 实体读写路径，
概览投影不包含 `apiKey`。

## 6. 单一写实现

`aiImplModule` 是 `AiOverviewQuery` / `AiCommands` 的唯一 Koin 绑定。架构护栏禁止 `:app`
导入这两个接口，也禁止在 `app/src/main/java/io/legado/app/feature/` 下重建适配器。

## 7. 验证

见 [`phase7-9-migration-record.md`](./phase7-9-migration-record.md)。

## 8. 回滚

只需把 `aiImplModule` 换回等价的 app 绑定；AI 表与 SSOT 不变。
关闭 `aiFeatureEnabled` 仍回到旧 AI 配置页，且不需要恢复 app adapter。

## 9. 删除条件

- 已删除：`LegacyAiAdapter`、对应 Koin 绑定、架构护栏中的 AI 兼容适配器白名单。
- 待删除（UI 转正后）：旧 AI 配置页 `ui/config/ai/AiConfigScreen.kt`、`USE_COMPOSE_AI_FEATURE`、
  `aiFeatureEnabled`。阻塞条件：取消、工具确认、错误恢复、密钥不泄漏的设备验收、
  Release/R8 产物与一个稳定版本周期的默认观察，均需人工签收，本次未发生。
- 待删除（AI 领域模型下沉后）：`AppAiDefaultModelHost`。

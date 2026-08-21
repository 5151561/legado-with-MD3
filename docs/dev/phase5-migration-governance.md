# Phase 5 旧路径删除与治理

> 状态：治理门禁已建立；当前没有 feature 获得旧路径删除许可
> 日期：2026-08-21

## 当前结论

Phase 2–4 的七个 Compose feature 均仍处于受控实验态。它们的迁移卡要求正式实现接缝、子流程
覆盖、设备矩阵、Release/R8 或稳定版本观察中的一项或多项；这些证据尚未全部签收，因此本阶段
不删除旧 UI、兼容适配器或构建期开关，也不把任何 Compose 入口改成默认。

统一登记表为 [`config/compose-feature-migrations.properties`](../../config/compose-feature-migrations.properties)。
`verifyFeatureMigrationGovernance` 已接入 `verifyConfigArchitecture`，编译和 assemble 任务会间接执行。

## 两条独立生命周期

实现状态与 UI 发布状态自 Phase 6 起独立登记，避免正式业务实现必须等待旧 UI 稳定观察才能落地：

```text
implementationStatus: app_adapter → formal_impl
uiStatus:             experiment → default_observation → complete
```

| 实现状态 | 运行时实现 | 必需证据 |
|---|---|---|
| `app_adapter` | `:app` 兼容适配器 | `compatAdapterPath` 存在、`implementationBlocker` 非空 |
| `formal_impl` | `feature:*:impl` | `formalImplModule`、契约/切换证据和适配器删除证据齐全；兼容适配器已删除 |

| UI 状态 | 默认入口 | 旧 UI | 临时开关 | 必需证据 |
|---|---|---|---|---|
| `experiment` | 旧 UI | 必须保留 | 必须登记且默认关闭 | 迁移卡、明确 `uiBlocker` |
| `default_observation` | 新 UI | 暂时保留 | 必须登记且默认开启 | `releaseGateEvidence` 指向设备/发布签收 |
| `complete` | 新 UI | 必须删除 | 必须删除 | 发布证据与 `legacyUiRemovalEvidence` 齐全，blocker 清零 |

UI 状态不能从 `experiment` 直接靠修改默认值跳到 `complete`。正式 `impl` 也不会自动推动 UI
状态：设备与发布门禁通过后才能进入 `default_observation`；稳定版本周期无回退后，再以独立变更
删除旧入口、XML/View、资源与开关。实现适配器可在 `formal_impl` 切换完成时提前删除。

## 自动治理范围

门禁会检查：

1. 所有 `USE_COMPOSE_*_FEATURE` BuildConfig 开关必须在登记表中，登记表也不能声明不存在的开关。
2. Gradle property、BuildConfig 常量与默认值必须完全一致，并拒绝重复或非标准声明，防止实验入口被误设为默认。
3. `app_adapter` 必须保留登记的兼容适配器；`formal_impl` 必须登记正式模块、切换证据和适配器删除证据，且两种实现不得共存。
4. 实验态和默认观察态必须保留登记的旧 UI、迁移卡、API/UI 模块和主源码开关消费点。
5. 每张迁移卡必须明确删除条件；两条状态线分别记录尚未满足的 `implementationBlocker` 与 `uiBlocker`。
6. 默认观察态必须提供仓库内的设备/发布签收文件；完成态还必须删除旧 UI 和临时开关，并提供可审计的删除签收文件。

## 当前登记

| Feature | 实现状态 | UI 状态 | 删除许可 | 主要 blocker |
|---|---|---|---|---|
| bookshelf | app_adapter | experiment | 否 | 正式 impl、Release/R8、设备矩阵 |
| settings | app_adapter | experiment | 否 | 偏好接缝、设置子页边界、设备矩阵 |
| catalog | app_adapter | experiment | 否 | 规则/网络/持久化接缝、子 route、设备矩阵 |
| rss | app_adapter | experiment | 否 | DAO/解析接缝、子 route、设备矩阵 |
| readaloud | app_adapter | experiment | 否 | 模块安全 Session Gateway、子页、设备矩阵 |
| reader | app_adapter | experiment | 否 | ReaderSession 模块化、功能 parity、性能与无障碍 |
| ai | app_adapter | experiment | 否 | 持久化/生成协议接缝、子 route、设备矩阵 |

## 删除执行清单

单个 feature 进入 `complete` 的同一变更必须：

1. 删除旧 Activity/Fragment/View/Compose 入口、XML、Adapter、Dialog、菜单与仅旧路径使用的资源。
2. 删除 app 临时适配器及其 Koin 绑定；若已建立正式 `impl`，保证单次运行只有一个 API 绑定。
3. 删除 BuildConfig 字段、Gradle property 消费点和主源码条件分支。
4. 搜索 feature 名、旧类名、旧 route 和资源 ID，确认没有生产引用。
5. 运行 API/UI/架构测试、Release/R8、设备矩阵和恢复验证。
6. 新增删除签收记录，列出搜索结果、构建产物、设备范围和回滚策略，再更新登记表为 `complete`。

阅读器仍受 [`reader-phase4-migration-card.md`](./reader-phase4-migration-card.md) 的更严格门禁约束；
不得因 Phase 5 治理建立而提前删除 `ReadView`。

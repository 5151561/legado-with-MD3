# Phase 5 旧路径删除与治理

> 状态：治理门禁已建立；bookshelf、rss、catalog、ai 的实现已转正，但当前仍没有 feature 获得旧路径删除许可
> 日期：2026-08-22

## 当前结论

Phase 2–4 的七个 Compose feature 的 **UI** 均仍处于受控实验态。它们的迁移卡要求子流程覆盖、
设备矩阵、Release/R8 或稳定版本观察中的一项或多项；这些证据尚未全部签收，因此不删除旧 UI
或构建期开关，也不把任何 Compose 入口改成默认。

**实现**状态是另一条线：bookshelf、rss、catalog、ai 已在 Phase 7–8 完成正式 `impl` 切换，
对应的 `:app` 兼容适配器已随切换证据一并删除；settings、readaloud、reader 仍由兼容适配器提供，
各自的 blocker 见下表。

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

| Feature | 实现状态 | UI 状态 | 删除许可 | 实现 blocker | UI blocker |
|---|---|---|---|---|---|
| bookshelf | formal_impl | experiment | 否 | 无 | Release/R8 复验、设备矩阵、稳定版本观察 |
| settings | app_adapter | experiment | 否 | `:core:preferences` 未建立；设置 Gateway 及其模型仍是 `:app` 领域类型 | 主题即时刷新、返回栈、大字体、TalkBack、稳定版本观察 |
| catalog | formal_impl | experiment | 否 | 无 | 规则语义、导入兼容、详情/发现恢复、稳定版本观察 |
| rss | formal_impl | experiment | 否 | 无 | JS 单 URL、WebView/外链、返回栈、稳定版本观察 |
| readaloud | app_adapter | experiment | 否 | 播放服务未暴露模块安全 Session API | 锁屏、耳机、中断、缓存、进程恢复、稳定版本观察 |
| reader | app_adapter | experiment | 否 | `ReadBook` 运行时单例未模块化；Phase 4 决策门未通过 | 专项 parity、帧率、内存、无障碍、稳定版本观察 |
| ai | formal_impl | experiment | 否 | 无（`setDefaultModel` 仍由 app 唯一写入，属登记在案的宿主接缝） | 取消、工具确认、错误恢复、密钥不泄漏、稳定版本观察 |

已转正 feature 的切换与适配器删除证据登记在 [`docs/dev/evidence/`](./evidence/)，
迁移卡见 [bookshelf](./bookshelf-phase7-migration-card.md)、[rss](./rss-phase8-migration-card.md)、
[catalog](./catalog-phase8-migration-card.md)、[ai](./ai-phase8-migration-card.md)。

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

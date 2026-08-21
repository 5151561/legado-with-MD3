# Phase 5 旧路径删除与治理

> 状态：治理门禁已建立；当前没有 feature 获得旧路径删除许可
> 日期：2026-08-21

## 当前结论

Phase 2–4 的七个 Compose feature 均仍处于受控实验态。它们的迁移卡要求正式实现接缝、子流程
覆盖、设备矩阵、Release/R8 或稳定版本观察中的一项或多项；这些证据尚未全部签收，因此本阶段
不删除旧 UI、兼容适配器或构建期开关，也不把任何 Compose 入口改成默认。

统一登记表为 [`config/compose-feature-migrations.properties`](../../config/compose-feature-migrations.properties)。
`verifyFeatureMigrationGovernance` 已接入 `verifyConfigArchitecture`，编译和 assemble 任务会间接执行。

## 生命周期

| 状态 | 默认入口 | 旧路径 | 临时开关 | 必需证据 |
|---|---|---|---|---|
| `experiment` | 旧实现 | 必须保留 | 必须登记且默认关闭 | 迁移卡、明确 blocker |
| `default_observation` | 新实现 | 暂时保留 | 必须登记且默认开启 | `gateEvidence` 指向设备/发布签收，稳定版本观察尚未结束 |
| `complete` | 新实现 | 必须删除 | 必须删除 | 保留 `gateEvidence`，新增删除签收记录，blocker 清零 |

状态不能从 `experiment` 直接靠修改默认值跳到 `complete`。新实现先完成迁移卡的设备与发布门禁，
进入 `default_observation`；稳定版本周期无回退后，再以独立变更删除旧入口、XML/View、适配器、
Koin 绑定、资源与开关，并把签收记录填入 `removalEvidence`。

## 自动治理范围

门禁会检查：

1. 所有 `USE_COMPOSE_*_FEATURE` BuildConfig 开关必须在登记表中，登记表也不能声明不存在的开关。
2. Gradle property、BuildConfig 常量与默认值必须完全一致，并拒绝重复或非标准声明，防止实验入口被误设为默认。
3. 实验态和默认观察态必须保留登记的旧适配器、迁移卡、API/UI 模块和主源码开关消费点。
4. 每张迁移卡必须明确删除条件，实验态必须记录尚未满足的 blocker。
5. 默认观察态必须提供仓库内的设备/发布签收文件；完成态还必须删除旧路径和临时开关，并提供可审计的删除签收文件。

## 当前登记

| Feature | 状态 | 删除许可 | 主要 blocker |
|---|---|---|---|
| bookshelf | experiment | 否 | 正式 impl、Release/R8、设备矩阵、稳定版本观察 |
| settings | experiment | 否 | 设置子页边界、设备矩阵、稳定版本观察 |
| catalog | experiment | 否 | 正式 impl、子 route、设备矩阵、稳定版本观察 |
| rss | experiment | 否 | DAO/解析接缝、子 route、设备矩阵、稳定版本观察 |
| readaloud | experiment | 否 | 模块安全 Session Gateway、子页、设备矩阵、稳定版本观察 |
| reader | experiment | 否 | 功能 parity、性能、内存、无障碍、稳定版本观察 |
| ai | experiment | 否 | 正式 impl、子 route、设备矩阵、稳定版本观察 |

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

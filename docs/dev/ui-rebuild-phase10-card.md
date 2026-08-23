# Phase 10 迁移卡：全新 UI 重做与旧 UI 删除

> 状态：线 A 进行中（catalog 已完成行为盘点与 api 扩面），线 B 部分完成。
> 日期：2026-08-22
> 上游计划：[`compose-ui-module-migration-plan.md`](./compose-ui-module-migration-plan.md) 第 5 节 Phase 10
> 取代：原「组件按需下沉与既有 Compose UI 迁入」路线（已作废，理由见第 1 节）

## 1. 前提变更与作废项

产品决定：**全 App UI 重做，使用全新组件库**。原路线（把既有 Compose 页面迁入
`feature:*:ui`、按需下沉 `app/ui/widget`）建立在"新 UI 复用既有组件、行为兼容迁入"的前提上，
该前提不再成立，整条路线作废。

作废清单：

| 作废项 | 规模 | 原因 |
|---|---|---|
| 迁入既有 Compose 页面 | 书架 6124 行，全 App `ui/` 179918 行 | 迁进来再删是纯浪费 |
| `app/ui/widget` 组件闭包下沉 | 212 文件 / 28933 行 | 新设计不复用这套组件 |
| 实验版 feature UI 追平旧 UI 功能 | 七个合计 2851 行 | 新设计页面形态本就不同 |
| `:core:designsystem` 的 11 个简化平行组件 | 432 行 | 由新组件库取代 |

已执行且不回滚：`AppText`、`PillDivider`、`PillHeaderDivider` 已迁入 `:core:designsystem`
（原批次 1，改写 139 个文件导入）。回滚会波及大量未提交改动且收益为零，
这三个文件随新组件库一并替换。

## 2. 关键结论：瓶颈是 API

七个 feature 的业务 API 合计 431 行：

| feature | api | impl | 实验版 ui |
|---|---:|---:|---:|
| bookshelf | 154 | 504 | 1052 |
| reader | 89 | 0 | 357 |
| rss | 49 | 180 | 347 |
| catalog | 43 | 140 | 303 |
| ai | 44 | 125 | 272 |
| readaloud | 36 | 0 | 261 |
| settings | 16 | 0 | 259 |

同期 `:app` 仍有 `ui/` 179918 行、`domain/` 11727 行、`data/` 14411 行、`model/` 14767 行，
`appDb` 直连 428 处。

**若不先扩面，新 UI 只能回头直连 `:app` 的 Repository/UseCase 或 `appDb`，
等于把刚拆掉的耦合重新长回来。** 这是本阶段唯一的关键路径。

## 3. 双线并行

```text
线 A（业务）  api 扩面 + impl 落地，逐 feature 推进    不需要设计稿即可开始
线 B（设计）  :core:designsystem 按新设计从头建        需要设计稿
线 C（组装）  feature 新 UI = 线 A 的 api + 线 B 的组件  依赖 A 与 B
线 D（清理）  整块删除旧 UI、实验版 UI 与开关           依赖 C
```

## 4. 硬约束：删除前必须先有契约

旧 UI 是这些页面"到底该做什么"的唯一记录。删除前必须先把行为抽成 `api` 契约与测试，
否则新 UI 会静默漏功能，且没有任何地方能发现。

每个 feature 的固定顺序：

1. **行为盘点**：旧 UI 的全部用户旅程，含异常路径、权限、Activity Result、进程恢复。
2. **api 表达 + 契约测试**，`impl` 落地。
3. **新 UI 建在 api 上**。
4. **整块删除**旧 UI 与实验版切片。

第 2 步完成前不删任何旧 UI。不因"反正要重写"而放宽。

## 5. 线 A 的书架扩面清单

> 顺序已调整，书架不再是线 A 的第一个 feature，见第 7 节。本节的清单继续有效。

当前 `bookshelf:api` 只覆盖查询、分组、删除、排序，需扩面的能力（来自旧 UI 行为盘点）：

| 能力 | 归属 | 说明 |
|---|---|---|
| 刷新书架 / 更新目录 | `bookshelf:api` | 书源抓取经 host 接缝，不把规则引擎拉进 impl |
| 批量缓存下载 | `bookshelf:api` 发命令 | 下载仍由 `CacheBookService` 执行，api 只表达命令与进度投影 |
| 书架导入 / 导出 / WebDAV 上传 | `bookshelf:api` | Uri 与文件权限留在 Route 边界 |
| 书架设置读写 | `bookshelf:api` | `:core:preferences` 建立前经 `BookshelfPreferencesHost` |
| 分组管理（增删改、排序、封面） | `bookshelf:api` | 见下方 tag 规则风险 |
| 拖拽排序 | 复用既有 `reorderBooks` | 无需新命令 |
| AI 自动分组 | 写命令归 `bookshelf:api` | 它写的是书架分组；AI 生成能力经 `ai:api` 消费，不得在 ai 侧复制分组写入 |

**已知回归风险**：Phase 7 迁移卡第 5 节把"分组增删改的 tag 规则重放"标记为
"移除（不适用）"，依据是书架 API 恒传 `pattern = null`。旧 UI 的分组编辑会传规则，
扩面时该分支必须重新纳入 api 与 impl 并补契约测试。

## 6. 线 B：`:core:designsystem` 从头建

设计稿已导入，规格见 [`ui-redesign-spec.md`](./ui-redesign-spec.md)：55 个画板，
种子色石墨青 `#35606E`，主题收敛为日光/夜墨/跟随系统三选一 + 用户可选强调色。

**已完成**：令牌层 `AppColorScheme.kt`、`ReadingPalette.kt`、`AppTypography.kt`，
通过 `:core:designsystem:compileDebugKotlin`。与现有 `LegadoColorScheme` 并存，互不影响。

**已完成**（续，2026-08-23）：间距与形状体系、组件 kit 与状态矩阵已落地并被八个重设计页面消费
（C-01、M-01/M-01a、P-01、D-00、S-04/S-04a、S-06a、S-06b），18 张截图基线在
`:feature:settings:ui`、`:feature:home:ui`、`:feature:catalog:ui` 三个模块里。
其中 `:feature:home:ui` 是本轮新建的 UI-only 模块——首页在模块图里没有对应 feature，
不登记进 `config/compose-feature-migrations.properties`（那张表管的是既有 Compose 页面的灰度），
线 A 排到首页时随卡一并登记。

**未完成**：自适应断点（X-01 平板三栏 / X-02 折叠屏双栏）、无障碍与大字体行为。

**关键结构决定**：正文纸色独立于 App 主题（画板 N-04 明确），由阅读样式抽屉单独选择。
现有实现把两者耦合在主题里，迁移时必须拆开。

现有 11 个简化平行组件（`AppScaffold`、`AppIcon`、`AppIconButton`、`AppModalBottomSheet`、
`AppTopBar`、`AppListItem`、`AppConfirmDialog`、`AppFeedback`、`AppCircularProgressIndicator`、
`PrimaryButton`、`SecondaryButton`）随新组件库落地一并删除。

## 7. 顺序

**已调整（2026-08-23）**：catalog 先行，书架顺延。

原顺序「书架先行」的依据是它 impl 基础最好。但首批落地的七块画板里没有书架页，
四块（D-00、S-04/S-04a、S-06a、S-06b）落在 catalog——先扩书架的 api 会得到一段
暂时没有 UI 消费的契约，而画板已经在等 catalog。catalog 同样已有正式 impl，
无实现 blocker，前提不劣于书架。

调整后：catalog → 书架（设计出书架页时）→ rss / ai（均已有 impl）
→ settings / readaloud（需先解除各自实现 blocker）→ **阅读器最后**。

catalog 的行为盘点见 [`catalog-behavior-inventory.md`](./catalog-behavior-inventory.md)。
书架扩面时，第 5 节记的 tag 规则重放回归风险继续有效。

阅读器不因"全 App 重做"绕过 Phase 4 决策门：`ReadView` 是 View 实现，
重做涉及分页、翻页、手势、选区、缓存与朗读，parity、帧率、内存与无障碍门禁继续有效，
见 [`reader-phase4-migration-card.md`](./reader-phase4-migration-card.md)。

## 8. 删除条件

单个 feature 完成时，同一变更中必须：

1. `api` 已覆盖该 feature 旧 UI 的全部行为并有契约测试通过。
2. 删除旧 UI 页面、实验版 `feature:*:ui` 切片、BuildConfig 开关、Gradle property
   与仅旧路径使用的资源。
3. 搜索旧类名、旧 route、资源 ID 确认无生产引用。
4. 架构门禁、单测、Release/R8 通过；按
   [`phase5-migration-governance.md`](./phase5-migration-governance.md) 的定义完成
   主力机 7 天观察后登记 `complete`。

## 9. 回滚

线 A 与线 B 各自可独立回退。线 C 未完成前旧 UI 保持默认，关闭 feature 开关即回退。
数据库、持久化格式与 SSOT 全程不变。

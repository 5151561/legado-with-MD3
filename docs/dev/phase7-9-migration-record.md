# Phase 7–9 实施记录

> 日期：2026-08-22
> 上游计划：[`compose-ui-module-migration-plan.md`](./compose-ui-module-migration-plan.md)

本文件记录 Phase 7–9 每一步的实际动作和未完成项，只写已经发生的事实，不预写未完成的签收。

逐 feature 的切换与适配器删除证据拆在 [`docs/dev/evidence/`](./evidence/)，
分别由迁移登记表的 `implementationEvidence` 与 `adapterRemovalEvidence` 引用：

| Feature | 实现切换证据 | 适配器删除证据 |
|---|---|---|
| bookshelf | [switch](./evidence/bookshelf-implementation-switch.md) | [removal](./evidence/bookshelf-adapter-removal.md) |
| rss | [switch](./evidence/rss-implementation-switch.md) | [removal](./evidence/rss-adapter-removal.md) |
| catalog | [switch](./evidence/catalog-implementation-switch.md) | [removal](./evidence/catalog-adapter-removal.md) |
| ai | [switch](./evidence/ai-implementation-switch.md) | [removal](./evidence/ai-adapter-removal.md) |

## Phase 7：书架正式实现

1. 新建 `:feature:bookshelf:impl`（`legado.feature.impl` convention plugin），依赖
   `:feature:bookshelf:api` 与 `:core:database`，不依赖 `:app`。
2. `LegacyBookshelfAdapter` 的 API 映射、错误分类、命令校验与 SSOT 组合迁入
   `DefaultBookshelfRepository`；Room 读写收敛到 `RoomBookshelfStore`。
3. `bookshelfImplModule` 成为书架四个 API 接口的唯一 Koin 绑定，由 `App.onCreate()` 加载；
   `appModule.kt` 的四条逐接口绑定已删除，只保留两个宿主接缝的注册。
4. 删除 `LegacyBookshelfAdapter` 与 `LegacyBookshelfAdapterMappingTest`；其断言已并入
   `BookshelfImplContractTest`，对正式实现执行同一套契约。
5. 架构护栏更新：删除书架兼容适配器白名单，新增"`:app` 禁止导入已建立正式 impl 的 feature
   API 接口"规则。
6. `Book.isLocal` 判定下沉为 `Book.isLocalBook()`（`:core:database`），
   `help/book/BookExtensions.isLocal` 改为委托，规则单一所有者。

详见 [`bookshelf-phase7-migration-card.md`](./bookshelf-phase7-migration-card.md)。

## Phase 8：逐个 feature 转正

### 实现模块化已完成

| Feature | impl 模块 | 删除的 app 代码 | 保留的宿主接缝 |
|---|---|---|---|
| bookshelf | `:feature:bookshelf:impl` | `LegacyBookshelfAdapter` + 其映射测试 | `BookshelfPreferencesHost`、`BookshelfBookRemovalHost` |
| rss | `:feature:rss:impl` | `LegacyRssAdapter`、`ResolveRssOpenTargetUseCase` | `RssSourceScriptHost`、`RssSourceRemovalHost` |
| catalog | `:feature:catalog:impl` | `LegacyCatalogAdapter` | `CatalogSourceRemovalHost` |
| ai | `:feature:ai:impl` | `LegacyAiAdapter` | `AiDefaultModelHost` |

宿主接缝都只做转发，不含 feature 业务规则，且每一个都在对应迁移卡里登记了删除条件。
`AiTaskType` 作为 `ai_task_presets.taskType` 的持久化词表移入 `:core:database` 的同名包，
FQN 不变。

迁移卡：[bookshelf](./bookshelf-phase7-migration-card.md)、[rss](./rss-phase8-migration-card.md)、
[catalog](./catalog-phase8-migration-card.md)、[ai](./ai-phase8-migration-card.md)。

四个 feature 的删除验证结果一致：以 `app`/`core`/`feature`/构建脚本/`config` 为范围搜索被删类型名，
剩余命中只有契约测试的 KDoc 说明与登记表的 `compatAdapterPath`（治理门禁要求登记且文件必须不存在），
没有生产引用。`app/src/main/java/io/legado/app/feature/` 下只剩 settings、readaloud、reader 三个目录。

### 实现模块化被阻塞

| Feature | 阻塞原因 | 解除条件 |
|---|---|---|
| settings | 三个设置 Gateway 与其模型是 `:app` 领域类型且被大量旧调用方消费；`:core:preferences` 未建立。若现在建 `impl`，它要么只剩 `combine + map` 的空壳，要么复制一份偏好读取规则 | 把 `AppConfigStore`、`PreferencesDsCompat`、`PreferKey` 与 `dataStore` 委托迁入 `:core:preferences`，并决定设置 Gateway 的最终所有者 |
| readaloud | 播放服务没有模块安全的 Session API，桥接必须引用 `ui.book.readaloud.player` 协调器 | 播放服务暴露与 UI 解耦的 Session Gateway |
| reader | `ReadBook` 是 app 内的可变运行时单例；Phase 4 决策门未通过 | 按 Phase 4 与阅读器专项计划推进 |

这三个 feature 没有建立空壳 `impl`：那会引入第二个读取或写入规则的所有者，违反计划第 3.3、3.4 节。

### UI 转正

七个 feature 全部仍为 `uiStatus=experiment`，默认入口未改变，旧 UI 与灰度开关未删除。
进入 `default_observation` 需要设备矩阵（手机、横屏/分屏、大字体、TalkBack、进程恢复）与
Release 产物的人工签收，进入 `complete` 还需要一个稳定版本周期的观察——这些无法由本仓库的
自动化产生，本次未伪造任何签收证据。

## Phase 9：`:app` 瘦身与治理

已完成：

1. `:app` 不再持有 bookshelf / rss / catalog / ai 的业务实现或逐接口 API 绑定；
   `app/src/main/java/io/legado/app/feature/` 下只剩 settings、readaloud、reader 三个适配器。
2. 新增根任务 `verifyMigrationGovernance`，聚合 `verifyConfigArchitecture`（含护栏夹具与迁移
   登记表校验）与全部子模块的 `verifyModuleDependencies`。CI 只需执行这一个任务。
3. 架构护栏新增 `formalImplBoundApis` 棘轮；已转正 feature 的 API 接口不得在 `:app` 中被导入。
4. 更新 `CLAUDE.md` 的模块图、依赖方向、convention plugin 与 Koin 装配约定，
   以及本计划的阶段状态与执行清单。

未完成（依赖剩余 feature）：

- 全局 `appDb` 兼容入口未删除，生产引用基线仍为 430。
- 旧 UI、临时 BuildConfig/Gradle 开关、重复资源未删除。

### 构建度量

本机、配置缓存命中状态下，以 `:app:assembleAppDebug` 为观测点：

| 改动位置 | 重新执行的 Kotlin 编译任务 | 耗时 |
|---|---|---|
| `:feature:bookshelf:impl` 实现文件（ABI 未变） | 只有 `:feature:bookshelf:impl:compileDebugKotlin` | 约 11s |
| `:app` 装配文件 | 只有 `:app:compileAppDebugKotlin` | 约 11s |

改动 impl 模块不再触发 `:app` 重编译，模块拆分没有让反馈变慢，因此本轮不需要合并模块。

## 自动验证结果（2026-08-22）

| 任务 | 结果 |
|---|---|
| `:feature:bookshelf:impl:testDebugUnitTest`（21 例） | 通过 |
| `:feature:rss:impl:testDebugUnitTest`（16 例） | 通过 |
| `:feature:catalog:impl:testDebugUnitTest`（9 例） | 通过 |
| `:feature:ai:impl:testDebugUnitTest`（8 例） | 通过 |
| 四个 feature 的 `api` / `ui` Debug 单测 | 通过 |
| `:core:database:testDebugUnitTest` | 通过 |
| 四个 impl 模块的 `lintDebug` | 通过 |
| `:app:testAppDebugUnitTest` | 通过 |
| `verifyMigrationGovernance`（含架构护栏、护栏夹具、迁移登记表、全部模块依赖检查） | 通过 |
| `:app:compileAppDebugKotlin` | 通过 |
| `:app:assembleAppDebug` | 通过 |
| `:app:assembleAppDebug`（七个灰度开关同时开启） | 通过 |
| `:app:assembleAppRelease`（R8 + 资源压缩） | 通过 |

未执行：设备矩阵、进程恢复、TalkBack 与大字体验收、稳定版本周期观察、仪器测试
（`connectedAndroidTest`）。它们是 UI 转正的前置门禁，需要真机与发布流程。

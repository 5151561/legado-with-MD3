# Compose UI 重做与业务模块化迁移计划

> 状态：Phase 0、Phase 1 已完成；Phase 2、Phase 3 实现与范围内自动验证已完成，灰度入口默认关闭，设备人工矩阵待发布前执行
> 日期：2026-08-21
> 范围：Android 端 Compose UI 的重做，以及为它提供稳定边界的业务模块化。**不包含** `modules/web/` Vue 前端重做，也不把内嵌 Ktor 服务改造成独立云端后端。

## 1. 目标与非目标

### 目标

将 Android UI 从“`ui` 包可直接触达数据、运行时单例与辅助类”的形态，逐步收敛为按业务能力组织的 Compose feature。每个新 Compose feature 只消费稳定的业务 API，并通过 UDF 与 SSOT 渲染状态。

目标能力如下：

1. 某个 UI feature 可以重写、测试和发布，而不需要理解 Room、文件系统、规则引擎或阅读器运行时的内部实现。
2. Android UI 与业务实现可独立演进；业务写入始终由唯一的 SSOT 反馈为 UI 状态。
3. 新增 Compose 页面遵循统一的 `Contract → ViewModel → Screen → Navigation` 形态，不再新增 UI → DAO / `appDb` 直连。
4. `:app` 逐步成为装配层，而非继续承载所有 feature 实现。
5. 迁移期间保留旧 View/Compose 页面与现有数据库、Intent、服务、备份格式的行为兼容；同一事实不出现新旧双写。
6. 迁移不是对旧实现的机械封装；对当前垂直切片内有证据、可验证且能减少状态所有者或代码路径的业务债，允许同步修正或简化。

### 非目标

- 不以“模块数量”作为完成条件，也不立即把所有 `data`、`domain`、`ui` 文件移动一遍。
- 不拆分现有 Room 数据库，不改数据库名、schema、备份格式或书源规则语义。
- 不在本计划中重启已经删除的阅读器 Track C 方案；阅读器另有严格的前置条件，见第 5 节 Phase 4。
- 不为单个页面创建空壳 UseCase、Repository 或 Gradle 模块。
- 不以 UI 重做为由顺带替换 Koin、Navigation 3、网络栈、文件存储或现有服务。
- 不把历史行为全部冻结成永久规范；确认属于缺陷、重复写入、错误所有权或无必要复杂度的逻辑，可以按第 3.4 节有控制地修改。

## 2. 现状与问题基线

项目已有可复用的基础：Compose、Navigation 3、Koin、UDF/SSOT、Material 3 主题和一批业务 UseCase 已经存在。新的计划必须利用这些基础，而不是另起一套架构。

但当前 `:app` 仍是主要单体：UI、数据、运行时模型、服务、规则与 DI 都在同一模块。`AppDatabase` 通过全局 `appDb` 暴露，并包含 `allowMainThreadQueries()`；这使移动目录本身不能形成边界。[`AppDatabase.kt`](../../app/src/main/java/io/legado/app/data/AppDatabase.kt)

同时有一些数据实体仍主动写入数据库，例如 `Book.save()` / `Book.delete()`；它们不能直接作为新的 UI 或 feature API 模型。[`Book.kt`](../../app/src/main/java/io/legado/app/data/entities/Book.kt)

已存在的架构护栏应继续作为迁移底线：

- `verifyConfigArchitecture` 与 `legacyUiDaoAccessBaseline` 已冻结既有 UI→DAO 债务；新迁移不得扩大它们。
- 阅读器核心的已完成与待续工作由 [`mad-modernization-plan.md`](./mad-modernization-plan.md) 维护。
- 已删除的 Compose 阅读器 Track C 只保留作历史记录，不得被误当作可直接继续的实施方案：[`track-c-compose-reader-plan.md`](./track-c-compose-reader-plan.md)。
- 全面的产品与页面优先级以 [`界面重设计需求文档.md`](../界面重设计需求文档.md) 为准；本计划只规定工程迁移顺序和边界。

## 3. 目标模块模型

模块按“业务能力”划分，`core` 只放确实跨业务复用的技术能力。避免建立一个新的超级 `:core:domain`、`:core:data` 或 `:common` 垃圾场。

```text
:app
  ├── :core:designsystem
  ├── :core:navigation
  ├── :core:database              （迁移初期保持单一 Room schema）
  ├── :core:preferences
  ├── :core:network
  ├── :core:rule-engine           （规则解析；复用 :modules:rhino）
  └── :feature:<name>[:api|:impl|:ui]
        ├── bookshelf
        ├── catalog                （书源、搜索、发现、导入）
        ├── reader
        ├── readaloud
        ├── rss
        ├── settings
        ├── backup-sync
        └── ai
```

并非所有 feature 都必须拆成三个 Gradle 模块。只有 UI 重做频繁、业务实现复杂、或会被其他 feature 消费的重型 feature 才使用下列形态：

```text
:feature:bookshelf:api   业务模型、Gateway、UseCase、命令与查询结果
:feature:bookshelf:impl  Room / 文件 / 网络 / 运行时实现，以及 Koin 绑定
:feature:bookshelf:ui    Compose Contract、ViewModel、Screen、feature 自己的导航 entries
```

`impl` 不是首个必须创建的模块。若既有 Repository/UseCase 仍位于 `:app`，或仍反向依赖
`ui` 类型，迁移初期由 `:app` 中的临时兼容适配器实现 `feature:*:api`；只有最小依赖接缝
已经下沉后，才把实现迁入 `feature:*:impl`。禁止通过让 `impl` 依赖 `:app` 来复用旧代码。

### 3.1 依赖规则

```text
:app ───────────────→ feature:*:ui、feature:*:impl、core:*
feature:*:ui ───────→ feature:*:api、core:designsystem、core:navigation
feature:*:impl ─────→ feature:*:api、core:database / preferences / network / rule-engine
feature:A ──────────→ feature:B:api（仅在确有业务协作时）

禁止：feature:*:ui → DAO / appDb / Room 实体 / feature:*:impl
禁止：feature:A → feature:B:impl
禁止：core:* → feature:*
禁止：feature:*:impl → :app
```

迁移期唯一的例外方向是 `:app → feature:*:api`：`:app` 可暂时承载一个薄兼容适配器，
把新 API 翻译为当前单体内的 Repository/UseCase 调用。该适配器属于装配兼容代码，不能
新增业务规则；同一个 API 在运行时只能绑定临时适配器或正式 `impl` 之一，不能双绑、双写。

`api` 暂不强求纯 JVM：若某个既有业务接口合理依赖 Android 类型，可先放在 Android library；但它不得泄漏 `Context`、`Activity`、`View`、DAO、Room 实体和可变运行时对象。是否改为纯 Kotlin 模块，应在该 feature 的实现需要时再决定。

### 3.2 模块职责

| 模块 | 公开内容 | 明确不放入 |
|---|---|---|
| `:app` | `Application`、Manifest、启动兼容、Koin 总装配、根导航聚合 | feature 业务规则、DAO 调用、页面实现细节 |
| `:core:designsystem` | 无业务依赖的 token、主题基础、`AppScaffold`、通用列表/输入/反馈组件 | `Book`、`Rss`、设置 Gateway、页面专用状态 |
| `:core:navigation` | 根导航协议与最小公共类型 | feature 的业务逻辑或 ViewModel |
| `:core:database` | Room schema、迁移、DAO、持久化基础设施 | UI DTO、业务决策、`appDb` 的跨模块公开入口 |
| `:core:rule-engine` | JS/JSONPath/JSoup/XPath 规则执行与通用解析能力 | 书架 UI、RSS UI、Activity/Fragment |
| `feature:*:api` | 领域语言、稳定查询/命令、Gateway、UseCase | Room 实体、文件路径实现、Compose 类型 |
| `feature:*:impl` | API 的实现、SSOT 接入、数据映射、后台协作 | `@Composable`、Screen、导航器 |
| `feature:*:ui` | Contract、ViewModel、Screen、Sheet/Dialog、feature entries | 直接 DAO/数据库访问、业务持久化、Android 服务控制细节 |

迁移期间，`:app` 可临时保留上一节所述的 API 兼容适配器；它必须有删除条件，并且不改变
`:app` 最终只承担装配与兼容职责的目标。

### 3.3 跨 feature 业务所有权

同一写命令只能有一个业务 API 所有者。其他 feature 可以消费只读投影，或显式依赖所有者的
`api`，不得为了页面方便复制一套写入 UseCase。首批约定如下：

- `bookshelf:api` 提供书架摘要中的阅读进度投影，但不拥有保存、恢复或云同步阅读进度的命令。
- 阅读进度写入、恢复与同步归 `reader:api`；在 `reader:api` 建立前继续走现有阅读器兼容路径。
- 书架删除、分组和排序归 `bookshelf:api`，其他 feature 需要这些能力时依赖该 API。

### 3.4 业务逻辑更新策略

行为基线用于发现回归，不代表旧实现必须原样保留。每个迁移卡把遇到的业务逻辑分为四类：

| 类型 | 处理方式 | 最低证据与验证 |
|---|---|---|
| 边界阻断或明确缺陷 | 在当前垂直切片内修复 | 有复现、失败测试、数据竞争/双写/主线程 IO 等代码证据；修复后测试通过 |
| 等价简化 | 可随迁移实施，用户可观察行为与持久化格式保持一致 | 先用测试固定输入、输出、错误和并发语义；实现应减少分支、状态所有者或重复路径 |
| 有意改变产品行为 | 单独列入迁移卡并评审，不得伪装成“重构” | 写清旧行为、新行为、理由、兼容影响、数据处理、回滚方式和验收用例；高风险变化使用独立开关 |
| 与当前用户旅程无关的系统重构 | 记录到后续清单，不捆绑进当前迁移 | 说明收益和依赖，另立垂直切片或专项计划 |

优先允许处理的问题包括：同一事实多处写入、实体自持久化、Repository 反向依赖 UI、
EventBus 与 Flow 重复表达同一状态、主线程数据库/文件访问、重复且不一致的业务规则、无法测试的
全局可变状态，以及可以用已有标准库或项目能力替代的冗长自制逻辑。

实施约束：

1. 修改范围必须服务于当前迁移卡的用户旅程或解除其模块边界，不能借机重写无关子系统。
2. 默认保持数据库 schema、备份格式、公开 Intent、Service 协议和书源规则语义；确需改变时升级为“有意改变产品行为”，单独评审和迁移。
3. 优先做净删除、合并状态所有者和缩短调用链；不得为了“以后可能复用”引入新的空壳层或双实现。
4. UI 重做、等价业务简化和产品行为改变尽量拆成可独立评审的提交，确保问题可定位、可回滚。
5. 旧实现存在缺陷时，测试应固定正确的目标行为和兼容边界，不得仅为了让旧测试继续通过而永久复制缺陷。
6. 新逻辑稳定后必须删除被替代的旧路径、临时分支和重复测试夹具；不能让“现代实现”变成第三条长期路径。

## 4. Compose 前端约定

新建或彻底重做的 Compose feature 采用下列最小结构；文件数按复杂度增减，不为形式拆分：

```text
feature/<name>/ui/
  <Name>Contract.kt       UiState、Intent、Effect、必要的 Sheet/Dialog
  <Name>ViewModel.kt      StateFlow + SharedFlow；唯一 onIntent 入口
  <Name>Screen.kt         无状态业务 UI
  <Name>RouteScreen.kt    仅在需权限、Activity Result、生命周期桥接时存在
  <Name>Navigation.kt     feature 向 :app 暴露的 route / entries
```

- `UiState` 与面向 Compose 的 item 模型使用 `@Stable`；集合在 ViewModel 的输出边界转换为 `ImmutableList`、`ImmutableSet` 或 `ImmutableMap`。
- `Screen` 只接收 `state`、`onIntent` 与导航/系统边界回调；不注入 ViewModel，不读 DAO，不直接发起业务协程。只有被明确指定为 effect 唯一收集者时，才接收 effect 流。
- `ViewModel` 负责把 `api` 输出映射为渲染状态，并通过 `_uiState.update { it.copy(...) }` 原子更新相关字段。
- 每条 effect 流只能有一个收集者，默认由 Route/兼容 Activity 通过 `LaunchedEffect(Unit)` 收集；若纯 UI effect 由 Screen 收集，Route 不得再次订阅同一流。文件、权限、剪贴板、外部 Intent 等 Android 操作始终保留在 Route/Activity 边界。
- Navigation 3 的根 back stack 仍由 `:app` 拥有。新 feature route 直接实现可序列化 `NavKey`，或实现 `:core:navigation` 中的非 sealed 标记协议；不得要求跨模块继承 `sealed MainRoute`。feature 暴露 entry 注册函数，`:app` 聚合 entries，并显式维护每个跨 feature route 的入栈、去重、启动 Intent 和恢复策略；feature UI 不反向依赖 `MainActivity`。
- `:core:designsystem` 先服务于既有 `LegadoTheme` 与 Material 3 体系。主题配置、动态色和用户偏好仍由 settings feature / app shell 解析，不把它们硬搬进纯 UI 组件库。

## 5. 阶段计划

每一阶段必须可以独立编译、验证和回滚。迁移按垂直用户旅程推进，禁止先全量移动 data、再全量移动 ViewModel、最后全量重写 UI。

### Phase 0：冻结行为与定义边界

实施产物：[`bookshelf-phase0-migration-card.md`](./bookshelf-phase0-migration-card.md)。该迁移卡完成 M0.1/M0.2 的行为、依赖、错误语义、effect 收集层和写命令所有权盘点；Phase 0 未改生产调用链。

**目的**：为后续迁移建立可信起点，避免“重做 UI 时顺手改变业务”。

工作项：

1. 为首批书架链路建立行为清单：书架加载、分组、删除、排序、导入入口、阅读进度更新、空态/错误态、返回栈和进程重建；每项标记为“保持、等价简化、有意改变或移除”。
2. 记录首批屏幕当前使用的 DAO、Repository、全局模型、Service、EventBus、Activity Result 与设置项；明确每项将由哪个 API/Effect 承接。
3. 保持现有 `verifyConfigArchitecture`、DAO 直连基线和阅读器边界测试为必经检查；新增代码不进入遗留基线。
4. 定义新 API 的错误语义与 UI 映射：可重试失败、不可恢复失败、空内容、加载中和部分完成，不能只返回布尔值。
5. 为每条一次性 effect 指定唯一收集层；为每个写命令指定唯一业务 API 所有者。
6. 盘点当前切片内的重复写入、反向依赖、全局可变状态、主线程 IO 和重复规则，按第 3.4 节决定本批修复或后续处理。

验收：

- 首批 feature 有一页可评审的“旧行为 → 保持/简化/改变/移除 → 新 Intent/Effect/API”映射。
- 任何有意行为改变都有单独的理由、兼容影响、测试与回滚项；未标记的行为默认保持兼容。
- 未改动 Room schema、备份格式、Service 协议和旧 UI。
- 现有相关单元测试与架构检查保持通过。

回滚：仅文档、测试和边界盘点，不改变生产调用链。

### Phase 1：建立最小 Design System、导航装配接缝与跨模块护栏

实施产物：新增 `:core:designsystem` 与 `:core:navigation`。前者持有 Legado 主题消费
契约、CompositionLocal、语义 token，以及 Scaffold、Top Bar、反馈态、列表项、确认对话框、
Modal Bottom Sheet、按钮、图标和进度组件；`:app` 的旧 `ui/theme` 与通用组件入口继续转发到
同一组 token/组件，动态色、用户偏好、字体与背景图解析仍由 app shell 注入。About 页面已直接
消费 Design System 进度组件。

根导航新增非 sealed `AppRoute` 协议；既有 `MainRoute` 接入该协议，`MainNavigator` 对未显式
注册策略的 route 直接报错，不再静默忽略。架构任务现扫描 `:app`、`core:*`、`feature:*` 的
全部 Kotlin source set 和模块构建依赖，并通过非法 feature UI 依赖夹具验证拒绝规则。

**目的**：让新 Compose 页面不再继续向 `ui/widget` 堆放业务组件，同时不触发全量视觉重构。

工作项：

1. 创建 `:core:designsystem` 前先抽出主题消费接缝：把 `LegadoTheme` 所需的 CompositionLocal、颜色/排版/token 类型和 provider 契约放入该模块；主题配置、动态色和偏好解析仍留在 app shell / settings，并把解析结果注入 provider。
2. 保留现有 `ui/theme` 作为兼容包装或转发入口，确保旧调用方无需反向依赖；禁止形成 `:app → core:designsystem → :app`。
3. 只迁移已被两个以上 Compose feature 使用、且不依赖业务对象的组件与 token。先提供最小公开面：Scaffold/top bar、反馈（loading/empty/error）、通用列表项、确认对话框/底部 sheet、按钮与图标、间距/形状/token。
4. 创建 `:core:navigation` 的最小非 sealed 协议。feature route 可直接实现 `NavKey`；`MainNavGraph` 聚合 feature entries，`MainNavigator` / app shell 继续显式拥有入栈、去重、深链、启动 Intent 和恢复策略，不为未知 route 保留静默 no-op。
5. 扩展架构护栏，使其扫描 `:app`、`core:*`、`feature:*` 的所有 Kotlin source set；另加构建依赖检查，拒绝 `feature:*:ui → core:database / feature:*:impl`、`feature:*:impl → :app` 和 `core:* → feature:*`。

验收：

- `:core:designsystem` 不能依赖 `data`、`domain`、`model` 或任意 feature。
- 旧 `ui/theme` 调用方与新 Design System 在同一主题值下渲染，不出现第二套视觉系统。
- 至少一个新页面使用它，而不是复制组件代码。
- `:app` 能编译并仍可注册旧页面与新 feature 页面。
- 架构检查覆盖新增模块，并通过一个测试夹具或专用测试证明禁止依赖会失败。
- 现有 route 的入栈、去重、深链、返回和进程恢复行为保持不变；新增 route 不会被导航器静默忽略。

回滚：新模块可从构建图移除；旧 `ui/theme` / `ui/widget` 仍是原行为来源。

### Phase 2：书架作为首个垂直切片

实施产物：[`bookshelf-phase2-migration-card.md`](./bookshelf-phase2-migration-card.md)。已建立
`:feature:bookshelf:api` 与 `:feature:bookshelf:ui`，由 `:app` 中受架构护栏约束的单一临时适配器
接入现有 Room Flow、Repository 和 UseCase。DAO 书架查询投影已移出 UI 包，书架导出 UseCase
也不再依赖 UI item；由于 `AppDatabase` 尚未下沉到合法的 core 接缝，本阶段没有创建会反向依赖
`:app` 的伪 `impl`。新入口由 `-PbookshelfFeatureEnabled=true` 启用，默认仍渲染旧页面。

**目的**：验证“Compose 前端 → feature API → SSOT → Room”的完整路径。书架是 P0 主路径，业务边界比阅读器稳定，适合作为首刀。

模块与职责：

| 模块 | 首批范围 |
|---|---|
| `:feature:bookshelf:api` | 书架摘要、书组、排序/筛选、删除、分组变更和只读阅读进度投影；不暴露 `Book` Room 实体，不拥有阅读进度写命令 |
| `:app` 临时适配器 | 首批把 `bookshelf:api` 翻译为现有 Repository / UseCase；不新增业务规则，不被 feature 模块依赖 |
| `:feature:bookshelf:impl` | 在最小数据/业务接缝完成下沉后替换临时适配器；以当前 Room 为 SSOT，不复制表、不双写，不依赖 `:app` |
| `:feature:bookshelf:ui` | 书架首页、分组/选择态、搜索/排序入口、删除确认与 UI item 映射 |

迁移规则：

1. 先在 `:app` 以薄适配器包住既有 Repository/UseCase，并绑定到 `bookshelf:api`；旧逻辑若只是复杂但可用，先建立契约测试再等价简化；若存在双写、错误所有权或明确缺陷，按第 3.4 节在当前切片修复。业务修改应落在现有业务实现或后续正式 `impl`，不能堆入兼容适配器；不可在本阶段重构全部图书实体。
2. 在创建正式 `bookshelf:impl` 前，先移除其所需 Repository/UseCase 对 `ui` 类型的反向依赖，并把最小持久化/业务依赖下沉到合法模块。正式 `impl` 不得依赖 `:app`；替换绑定后删除临时适配器。
3. 新 UI item 使用稳定 ID、不可变集合和明确的 loading/empty/error/content 状态；不将 Room 实体直接传到深层 composable。
4. 删除、分组和排序写入由 `bookshelf:api` 命令完成，UI 只等待 SSOT 的新状态；阅读进度仅作为书架摘要投影，写入继续由阅读器唯一负责。禁止 optimistic 与旧路径同时写入。
5. `:app` 负责书架 route / entry 的根导航策略、Koin 总装配和旧入口兼容；除临时 API 适配器外，不在 `MainNavGraph` 或其他装配代码中查询 DAO。

验收：

- 新书架页不导入 `appDb`、DAO、`data.entities.Book` 或 `model` 运行时单例。
- 若仍使用 `:app` 临时适配器，其代码位置、允许调用和删除条件已有架构测试或清单约束；若已建立正式 `impl`，依赖图中不存在 `impl → :app`。
- 删除、分组和排序命令的成功/失败/重复点击有单元或集成测试；阅读器写入进度后，书架只读投影能由 SSOT 正确刷新；数据库迁移和现有书架数据兼容。
- 手机、横屏/分屏、大字体、TalkBack 的基本书架旅程通过人工验收。
- 用户可在问题发生时切回旧页面或关闭新入口；两条 UI 不双写数据。

### Phase 3：按业务域扩展 Compose feature

实施产物：settings、catalog、rss、readaloud、ai 均已建立独立 `api / ui` 模块、UDF
入口、单一 app 兼容适配器与独立构建期开关；对应迁移卡如下：

- [`settings-phase3-migration-card.md`](./settings-phase3-migration-card.md)
- [`catalog-phase3-migration-card.md`](./catalog-phase3-migration-card.md)
- [`rss-phase3-migration-card.md`](./rss-phase3-migration-card.md)
- [`readaloud-phase3-migration-card.md`](./readaloud-phase3-migration-card.md)
- [`ai-phase3-migration-card.md`](./ai-phase3-migration-card.md)

五个入口默认关闭，分别由 `settingsFeatureEnabled`、`catalogFeatureEnabled`、
`rssFeatureEnabled`、`readAloudFeatureEnabled`、`aiFeatureEnabled` 启用。新 UI 不导入 DAO、
Room 实体、`appDb`、Service 或 app 运行时对象；外部 Intent、根导航、阅读器 sheet 与现有
子流程仍由 app host 承接。写命令继续由现有 Gateway/Repository/播放协调器唯一执行并从 SSOT
回流，不改设置格式、Room schema、规则执行器、WebView 安全模型、播放服务或 AI 协议。

**目的**：复制已验证的形态，而不是为每个页面重新发明架构。

建议顺序与范围：

| 顺序 | Feature | 优先理由 | 首批包含 | 不包含 |
|---:|---|---|---|---|
| 3.1 | `settings` | 已有 Gateway/SSOT，适合稳定 UI API | 设置首页、主题/界面、下载缓存、备份入口 | 在同一批改设置存储格式 |
| 3.2 | `catalog` | 书源、搜索、发现、导入可共享业务 API | 搜索、发现、书籍详情、导入流程 | 规则执行器重写 |
| 3.3 | `rss` | 与书籍域相对独立 | RSS 首页、文章列表、收藏 | WebView 安全模型重写 |
| 3.4 | `readaloud` | 服务和 UI 需明确桥接 | 播放控制、语音选择、缓存页 | 替换播放服务或媒体会话 |
| 3.5 | `ai` | 可独立交付、状态复杂 | 配置、会话、生成结果展示 | 更换模型供应商或协议 |

每个 feature 都先提交一份小型“迁移卡”：旧入口、业务 API 与写命令所有者、UiState/Intent/Effect
及 effect 唯一收集层、导航策略、业务逻辑分类（保持/等价简化/有意改变/移除）、兼容项、测试、
Release 验证和删除条件。只有前一个 feature 的边界和验收稳定后，才复制到下一个 feature。

2026-08-21 自动验证记录：五个 API/UI 模块的 Debug 单测、五个 UI 模块 Lint、
`verifyConfigArchitecture`、全部五个灰度开关同时启用的 `:app:compileAppDebugKotlin` 与
`:app:assembleAppDebug` 均通过。Release/R8 与手机、横屏/分屏、大字体、TalkBack、进程恢复
仍属于发布前人工/发布构建门禁，不以本次 Debug 自动验证替代。

### Phase 4：阅读器 Compose 重做的独立决策门

**目的**：避免把最复杂的渲染与运行时系统当作普通页面迁移。

阅读器进入 Compose 重做前，必须单独满足以下条件：

1. `reader:api` 是阅读进度写入、恢复和同步命令的唯一所有者，并能表达只读阅读快照、章节/进度命令、加载失败与恢复；不泄漏可变 `ReadBook`、`TextPage` 或 DAO。
2. 分页、翻页、手势、选区、缓存、朗读和配置更新的现有行为都有可执行的 parity 基线。
3. 单一渲染所有权明确；禁止在同一默认路径上让旧 `ReadView` 与 Compose 对同一正文双重绘制。
4. 新 renderer 先从无动画、只读、可 feature-flag 回退的最小闭环开始；翻页动画、复杂选区和性能优化最后做。
5. 任何恢复本项的计划必须重新评审，不得默认延续已删除的 Track C。

验收：以阅读器专门计划定义的功能 parity、内存、帧率、无障碍和回滚开关为准。未通过前，旧阅读器保持默认实现。

### Phase 5：删除旧路径并强化治理

**目的**：让迁移带来真实的维护收益，而不是长期双轨。

删除条件：

1. 新 feature 已覆盖约定的用户旅程、异常路径、权限/Activity Result 和恢复场景。
2. 新旧 UI 已在一个稳定版本周期内不再同时作为默认入口；数据所有权始终唯一。
3. 旧 XML、Adapter、Dialog、ViewBinding、Koin 注册、菜单资源、兼容 Activity 和临时 flag 均无引用。
4. 相关架构测试、单元测试、设备验证与发布构建通过。

完成后删除旧路径及迁移适配器，更新本计划的状态、模块所有权和对应功能文档。禁止为了“以后可能要回滚”永久保留双实现。

## 6. Koin、导航与兼容策略

### Koin

- 每个正式 `feature:*:impl` 暴露自己的 Koin module，绑定 `api` 接口到实现；每个 `feature:*:ui` 暴露其 ViewModel module。
- `:app` 是唯一的总装配点，负责加载 feature modules。feature 之间不得 `GlobalContext.get()` 查找另一个 feature 的实现。
- 迁移期间若既有 Repository/UseCase 仍在 `:app`，由 `:app` 内的临时适配器实现 feature API；`impl` 不得调用或依赖 `:app` 中的 legacy implementation。
- 同一个 API 在单次运行中只能加载临时适配器或正式 `impl` 的一个 Koin 绑定；切换绑定前后都由同一 SSOT 回流状态。

### Navigation 3

- 根 back stack 与跨 feature 的顶层导航仍由 `:app` 维护。
- feature 自己声明直接实现 `NavKey`（或非 sealed 公共标记）的可序列化 route 与 entries，并以回调向上请求跨 feature 跳转；Screen 不持有全局 navigator。
- `:app` 为每个 feature route 显式注册入栈、去重和根栈重建策略；需要兼容启动的 route 同时注册 Intent extra 解析。不得依赖一个吞掉未知 route 的默认分支。
- 旧 Activity 或 Intent 入口仅保留为兼容 host：解析 extras、转换为 route、处理结果/权限，然后把业务与状态交给 Compose feature。
- 重写页面时，深链、返回、启动 route 和进程恢复均视为功能，不因“只是 UI 改造”而省略。

## 7. 验证与发布矩阵

| 范围 | 最低验证 | 完成判定 |
|---|---|---|
| Feature API | 正常、空、失败、重试、重复命令的单元测试 | 写入后由 SSOT 回流新状态 |
| 业务逻辑更新 | 等价简化的契约测试；行为改变的旧/新用例、兼容与回滚测试 | 未声明行为不回归；声明改变达到新验收且旧路径已删除或隔离 |
| 模块边界 | 扫描全部 source set 的架构检查 + Gradle 项目依赖检查 | UI 不可见 DAO / `appDb` / impl；impl 不依赖 app；core 不依赖 feature |
| Compose UI | UiState 状态测试、关键交互测试 | loading/content/empty/error/selection 可渲染 |
| 导航与系统交互 | 返回栈、Intent 兼容、权限/文件结果手测或设备测试 | 旧调用方不回归 |
| 数据 | 现有迁移与备份/恢复相关测试 | 不改 schema 与备份兼容性 |
| 无障碍与自适应 | 手机、横屏/分屏或平板、大字体、TalkBack | 关键操作可达且状态可理解 |
| 构建与发布 | 日常提交执行 `:app:compileAppDebugKotlin`；资源/Manifest 变更时执行 `:app:assembleAppDebug`；每个 feature 灰度/替换入口前执行 `assembleAppRelease` | CI 现有 test / lint / 架构检查均通过，Release/R8 构建可安装 |

每次执行 Gradle 验证后，只停止本次任务启动的 Gradle daemon 或相关高内存进程；不得影响用户已有开发进程。

## 8. 首批执行清单

以下是建议的实际起点，按顺序评审和提交：

1. **M0.1**：为书架写迁移卡与行为基线；列出旧 Screen/Activity、数据入口、事件、兼容调用方和测试，并把业务逻辑标记为保持、等价简化、有意改变或移除。
2. **M0.2**：定义 `bookshelf:api` 的查询、命令、错误模型、写命令所有权和 UI 映射；不移动业务代码。
3. **M1.1**：建立 `:core:designsystem` 的主题消费接缝、最小模块与无业务依赖组件；保留旧 `ui/theme` 兼容包装，让一个非核心小页面试用。
4. **M1.2**：建立 feature entry 聚合与根导航策略接缝；保持 `MainNavGraph` / `MainNavigator` 的所有现有路由行为。
5. **M1.3**：扩展架构扫描到所有模块，并增加 Gradle 禁止依赖检查。
6. **M2.1（已完成）**：建立 `:feature:bookshelf:api`，由 `:app` 薄适配器接入既有 SSOT；未创建依赖 `:app` 的 `impl`。
7. **M2.2（已完成）**：实现新的书架 Compose Screen 与 UDF Contract，默认不替换旧入口。
8. **M2.3（已完成当前前置）**：书架 DAO/Repository 与导出 UseCase 已移除对书架 UI 类型的反向依赖；正式 `impl` 等 `AppDatabase` 合法下沉后创建。
9. **M2.4（范围内自动验证完成，设备矩阵待发布前执行）**：灰度编译开关、回退路径、命令/SSOT/架构测试和删除条件已建立；Debug APK、书架模块 Lint/单测已通过，全 App 既有测试/Lint 阻塞与未执行的 Release/R8 已记录在迁移卡；手机、横屏/分屏、大字体、TalkBack 仍按迁移卡逐项签收。
10. **M3.1（已完成）**：建立 settings API/UI、设置摘要 SSOT 投影、根导航入口与独立回退开关。
11. **M3.2（已完成）**：建立 catalog API/UI、书源查询/命令边界、发现/搜索/导入兼容导航与独立回退开关。
12. **M3.3（已完成）**：建立 RSS API/UI、打开目标解析 UseCase、RSS 写命令边界与独立回退开关。
13. **M3.4（已完成）**：建立 readaloud API/UI、唯一播放协调器桥接、控制/语音/缓存入口与独立回退开关。
14. **M3.5（已完成）**：建立 AI API/UI、profile/preset SSOT 投影、默认模型命令与会话/生成兼容入口。

## 9. 决策记录与待确认项

在实施 M0.1 前确认以下决定：

1. **首个 UI 切片**：默认选择书架；若产品近期重点是设置或书源，可替换，但必须重新写行为基线。
2. **模块粒度**：书架的目标形态是 `api / impl / ui`；首批只强制建立 `api / ui`，`impl` 在依赖接缝下沉后创建。后续 feature 根据复杂度决定，不预建空模块。
3. **新 UI 的上线方式**：首批采用开发开关、灰度入口还是直接替换。默认建议先保留可回退入口。
4. **阅读器范围**：本计划不承诺阅读器的完整 Compose 重写日期；只有第 5 节 Phase 4 的门槛满足后才立项。
5. **业务更新授权**：默认允许当前切片内的缺陷修复和有测试保护的等价简化；改变产品行为、数据格式或跨 feature 语义时必须单独确认。

## 10. 追溯表

| 目标问题 | 对应阶段 | 可验证结果 |
|---|---|---|
| UI 穿透数据和运行时实现 | Phase 0、2、3 | 新 UI 无 DAO / `appDb` / Room 实体导入 |
| `:app` 承载过多业务与页面装配 | Phase 1、2、3 | feature 最终自带 UI/API/实现；app 只保留根导航、总装配和有删除条件的迁移适配器 |
| 组件与主题重复、重做 UI 容易漂移 | Phase 1 | Design System 无业务依赖且被多个页面复用 |
| Compose 重写改变既有行为 | Phase 0、2、3 | 行为基线、SSOT 回流、导航/恢复测试通过 |
| 迁移只包裹旧债、没有降低复杂度 | Phase 0、2、3、5 | 业务逻辑分类明确；等价简化有契约测试；替代路径按删除条件清理 |
| 阅读器复杂度导致大爆炸迁移 | Phase 4 | 单独 decision gate、parity 基线和可回退路径 |
| 临时双轨长期残留 | Phase 5 | 有删除条件、搜索验证和旧资源清理 |

# 书架 Compose 垂直切片实施卡（Phase 2）

> 状态：实现与 Phase 2 范围自动验证完成；灰度入口默认关闭；设备人工矩阵待发布前执行
> 日期：2026-08-21
> 对应计划：[Compose UI 重做与业务模块化迁移计划](./compose-ui-module-migration-plan.md)

## 1. 已交付边界

- `:feature:bookshelf:api`：稳定书籍摘要、书组、查询状态、偏好投影、删除/移动/排序与书组命令；不含 Android、Compose、Room、DAO、`Book` 实体或阅读进度写命令。
- `:feature:bookshelf:ui`：`Contract → ViewModel → RouteScreen → Screen` UDF；使用不可变集合、稳定 ID、单一 effect 收集层和 loading/content/empty/error 四态。
- `:app` 临时接缝：[`LegacyBookshelfAdapter.kt`](../../app/src/main/java/io/legado/app/feature/bookshelf/compat/LegacyBookshelfAdapter.kt) 是唯一兼容适配器，查询从现有 Room Flow 回流，写命令只调用既有 Repository/UseCase/Gateway。
- 根装配：`:app` 仍拥有 Koin 总装配、书架所在根分页、打开阅读器/详情、全局搜索、导入和管理页导航。
- 没有创建 `:feature:bookshelf:impl`。当前 Room schema 与 `AppDatabase` 仍在 `:app`，创建依赖 `:app` 的空壳 impl 会违反既定方向。

数据库 schema、备份格式、Intent、Service、EventBus 和阅读进度写入所有权均未改变。

## 2. SSOT 与写入语义

- `BookshelfQuery` 每次订阅先发 `Loading`，再由书籍 Flow 与书组 Flow 组合为 `Data`；异常明确映射为可重试/权限/参数/未知错误，不再把失败伪装为空内容。
- Screen 不直接增删书籍。删除、移动、排序成功后仍保留当前快照，直到 Room Flow 发出新 snapshot。
- 同一时刻只允许一个书架写命令；重复确认不会触发第二次写入。
- 删除异常后根据 SSOT 中仍存在的 ID 映射 `Failure` 或 `Partial`；成功项和失败项分别表达。
- 阅读进度只存在于 `BookshelfBookSummary.readingProgress` 查询投影。新 API 没有保存阅读进度的命令。
- 手动书籍/书组排序先验证重复 ID 和缺失 ID，再提交完整顺序，失败时不执行部分顺序写入。

## 3. UI 与 Design System 组件计划

| 页面元素 | 实现 | 组件来源 |
|---|---|---|
| 页面模板、Top Bar、反馈态 | `AppScaffold`、`AppTopBar`、`AppFeedback` | `:core:designsystem` |
| 书籍行、移动分组行 | `AppListItem` | `:core:designsystem` |
| 删除确认、移动 Sheet、图标按钮 | `AppConfirmDialog`、`AppModalBottomSheet`、`AppIconButton`、`AppIcon` | `:core:designsystem` |
| 分组筛选、搜索、菜单、进度 | `FilterChip`、`OutlinedTextField`、`DropdownMenu`、`LinearProgressIndicator` | Material 3 |
| 书籍封面与书架行组合 | 页面内 molecule | Coil `AsyncImage` + Design System list item；无可复用的公共业务组件 |

页面内自定义只负责书籍封面、阅读进度和书籍字段编排；颜色、排版、间距和反馈状态均消费 `LegadoTheme` 或现有组件。列表使用稳定 `bookUrl` key 和固定 content type；长按详情、选择状态、图标描述和最小触控目标沿用 Material/Design System 语义。

## 4. 入口、灰度与回退

默认构建继续使用旧书架。启用新入口：

```bash
./gradlew :app:assembleAppDebug -PbookshelfFeatureEnabled=true
```

开关是构建期单选，单次运行只会渲染一个书架页面；两条 UI 共用同一数据库和既有写入 UseCase，不存在新表或双写。出现问题时移除该 Gradle property 即回到旧页面。

## 5. 架构护栏与删除条件

`verifyConfigArchitecture` 新增以下规则：

- feature API 禁止导入 Android/Compose/Room 或 app 的 data/domain/model/ui 实现类型；
- feature UI 禁止依赖 `:app`、`core:database`、任意 feature impl，也禁止 DAO/entity/`appDb` 导入；
- app 的书架 data/domain 禁止重新依赖旧书架 UI 类型；
- 临时兼容目录只允许一个 `LegacyBookshelfAdapter.kt`，并禁止扩张到 DAO、`appDb`、Config、Service、运行时 model 或 UI；
- 非法 Gradle 依赖夹具同时验证 UI/API 规则确实会失败。

临时适配器的删除条件：

1. `AppDatabase`/DAO 与书架查询、命令所需的最小持久化接缝下沉到合法模块；
2. 新建不依赖 `:app` 的 `:feature:bookshelf:impl`，完成同一套 API 契约与 SSOT 测试；
3. Koin 总装配把四个 API 接口一次性切换到正式 impl，删除 `LegacyBookshelfAdapter` 和其四个绑定；
4. 搜索确认不存在 `feature/bookshelf/compat` 引用，且 Release/R8 构建通过。

## 6. 验证记录

自动检查覆盖：

- API 的空内容与失败分离、阅读进度只读契约；
- ViewModel 的 loading/empty/error/content、删除重复点击、失败保留选择、成功等待 SSOT、阅读进度投影刷新；
- 删除 partial/failure 映射；
- 书籍与书组排序的正常、重复 ID、缺失 ID 和无部分写入；
- 既有 `DeleteBooksUseCaseTest`、`UpdateBooksGroupUseCaseTest`；
- `verifyConfigArchitecture` 与 app 编译。

2026-08-21 验证结果：API/UI 单测、书架 UI Lint、架构守卫与启用灰度开关的
`:app:assembleAppDebug` 均通过。仓库全量 App 单测仍有一个与本切片无关的既有失败：
`ThemeSettingsMappingTest` 将主题键数量固定为 69，当前实现返回 68；全 App Lint 仍有一个
与本切片无关的既有 `AudioPlayService` Media3 opt-in 错误。Release/R8 按本次验证范围未执行，
仍保留为删除临时适配器与正式发布前的门禁。

发布前仍需在启用开关的包上人工签收：

- [ ] 手机竖屏：加载、切组、搜索、排序、打开书籍/详情、选择、移动、删除、导入、管理与返回；
- [ ] 横屏/分屏或平板：内容不截断、Top Bar 和 Sheet 可操作；
- [ ] 系统大字体：标题、作者、章节和对话框可换行且关键操作不丢失；
- [ ] TalkBack：分组、书籍、进度、选择状态、菜单和删除确认可理解且顺序正确；
- [ ] 进程重建：根 back stack 恢复，书架内容与保存分组从 SSOT 重建；
- [ ] 启用/关闭开关各构建一次，确认旧入口可回退且无数据双写。

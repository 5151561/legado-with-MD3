# bookshelf 兼容适配器删除证据（Phase 7）

> 日期：2026-08-22
> 迁移卡：[`bookshelf-phase7-migration-card.md`](../bookshelf-phase7-migration-card.md)
> 前置：[`bookshelf-implementation-switch.md`](./bookshelf-implementation-switch.md)

## 删除的文件

- `app/src/main/java/io/legado/app/feature/bookshelf/compat/LegacyBookshelfAdapter.kt`
- `app/src/test/java/io/legado/app/feature/bookshelf/compat/LegacyBookshelfAdapterMappingTest.kt`

## 搜索验证

以 `app`、`core`、`feature`、`build.gradle.kts`、`settings.gradle`、`config` 为范围
搜索被删类型名，剩余命中仅两类，均非生产引用：

1. `:feature:bookshelf:impl` 契约测试的 KDoc 说明（记录这些断言来自被删的适配器）；
2. `config/compose-feature-migrations.properties` 的 `bookshelf.compatAdapterPath` 登记项 ——
   治理门禁要求 `formal_impl` 状态下该路径**必须登记且文件必须不存在**，用于防止适配器被悄悄加回。

`app/src/main/java/io/legado/app/feature/` 下已无 `bookshelf` 目录。

附带删除：`BookshelfRepository`、四个 UseCase 与 `BookGroupMutationRepository` **未**删除，
它们仍服务旧书架 UI 与其它调用方；被删除的只有书架 API 的适配器实现本身。

## 护栏变更

- `verifyConfigArchitecture` 的 `phase3CompatFiles` / `allowedCompatFiles` 白名单已移除 `bookshelf`，
  在 `app/src/main/java/io/legado/app/feature/bookshelf/` 下新增任何文件都会失败。
- `formalImplBoundApis` 已收录 `BookshelfQuery`、`BookshelfPreferencesGateway`、`BookshelfCommands`、`BookshelfGroupCommands`，`:app` 不得再导入。

## 未删除的部分

旧 `bookshelf` UI、`uiStatus` 与灰度开关不在本次删除范围内，仍按 Phase 5 生命周期由
[`phase5-migration-governance.md`](../phase5-migration-governance.md) 管理。

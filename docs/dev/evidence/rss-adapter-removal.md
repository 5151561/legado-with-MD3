# rss 兼容适配器删除证据（Phase 8.3）

> 日期：2026-08-22
> 迁移卡：[`rss-phase8-migration-card.md`](../rss-phase8-migration-card.md)
> 前置：[`rss-implementation-switch.md`](./rss-implementation-switch.md)

## 删除的文件

- `app/src/main/java/io/legado/app/feature/rss/compat/LegacyRssAdapter.kt`
- `app/src/main/java/io/legado/app/domain/usecase/ResolveRssOpenTargetUseCase.kt`

## 搜索验证

以 `app`、`core`、`feature`、`build.gradle.kts`、`settings.gradle`、`config` 为范围
搜索被删类型名，剩余命中仅两类，均非生产引用：

1. `:feature:rss:impl` 契约测试的 KDoc 说明（记录这些断言来自被删的适配器）；
2. `config/compose-feature-migrations.properties` 的 `rss.compatAdapterPath` 登记项 ——
   治理门禁要求 `formal_impl` 状态下该路径**必须登记且文件必须不存在**，用于防止适配器被悄悄加回。

`app/src/main/java/io/legado/app/feature/` 下已无 `rss` 目录。

`ResolveRssOpenTargetUseCase` 与 `LegacyRssOpenTarget` 的唯一调用方就是被删的适配器，
因此随之整体删除；打开目标解析仍是单一所有者。

## 护栏变更

- `verifyConfigArchitecture` 的 `phase3CompatFiles` / `allowedCompatFiles` 白名单已移除 `rss`，
  在 `app/src/main/java/io/legado/app/feature/rss/` 下新增任何文件都会失败。
- `formalImplBoundApis` 已收录 `RssQuery`、`RssCommands`，`:app` 不得再导入。

## 未删除的部分

旧 `rss` UI、`uiStatus` 与灰度开关不在本次删除范围内，仍按 Phase 5 生命周期由
[`phase5-migration-governance.md`](../phase5-migration-governance.md) 管理。

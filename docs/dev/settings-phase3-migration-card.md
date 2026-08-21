# Settings Compose 迁移卡（Phase 3.1）

> 状态：实现与范围内自动验证完成；灰度入口默认关闭；设备人工矩阵待发布前执行

## 边界与入口

- 旧入口：`MainRouteSettings → ConfigNavScreen`。
- 新入口：`MainRouteSettings → :feature:settings:ui`，由 `-PsettingsFeatureEnabled=true` 单选启用。
- `:feature:settings:api` 只公开主题模式、字号、缓存与备份状态的只读投影；设置写入仍由各 SettingsGateway 唯一拥有。
- `LegacySettingsAdapter` 只组合 AppShell、DownloadCache 与 Backup 三条现有 SSOT Flow，不读偏好、不新增写路径。

## UDF、Effect 与兼容

- `SettingsViewModel` 是 UiState 所有者；重试会取消旧收集，避免重复订阅。
- `SettingsRouteScreen` 是 effect 唯一收集者；主题、界面、阅读、封面、缓存、备份、AI、翻译和实验室入口回调给 app 根导航。
- 保持设置存储键、格式、动态色和主题解析不变；新页面只是入口与摘要重做。

## 删除条件

各设置子页建立自己的 feature 边界后，删除 `LegacySettingsAdapter`、旧 `ConfigNavScreen` 和灰度开关；发布前验证返回栈、主题即时刷新、大字体与 TalkBack。

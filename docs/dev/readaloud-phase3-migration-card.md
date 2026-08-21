# Read-aloud Compose 迁移卡（Phase 3.4）

> 状态：实现与范围内自动验证完成；灰度入口默认关闭；设备人工矩阵待发布前执行

## 边界与入口

- 旧入口：阅读器 `ReadAloudPlayerScreenContent`。
- 新入口：`:feature:readaloud:ui`，由 `-PreadAloudFeatureEnabled=true` 在同一 reader sheet 内单选启用。
- API 公开进程内朗读会话快照与控制命令；不公开 `ReadBook`、Service、EventBus 或播放器 UI 类型。
- `LegacyReadAloudAdapter` 是唯一服务桥，复用现有 `ReadAloudPlayerCoordinator`；播放服务、媒体会话和缓存协议均未替换。

## UDF、Effect 与兼容

- Screen 持有播放/暂停、段落/章节、进度、语音、缓存与设置入口；命令由 ViewModel 串行化。
- Route 是 effect 唯一收集者；语音选择、缓存页、阅读设置和经典控制回调给现有 reader host。
- 播放状态继续由 `ReadAloudSessionStore`/Coordinator 回流，不新增第二个播放状态所有者。

## 删除条件

播放服务提供模块安全的 Session Gateway 后删除协调器适配器；语音与缓存子页迁入 feature 后删除旧播放器与灰度开关，并完成锁屏/耳机/中断/进程恢复测试。

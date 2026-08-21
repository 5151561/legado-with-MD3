# RSS Compose 迁移卡（Phase 3.3）

> 状态：实现与范围内自动验证完成；灰度入口默认关闭；设备人工矩阵待发布前执行

## 边界与入口

- 旧入口：主页 RSS 分页的 app 内 `RssRouteScreen`。
- 新入口：`:feature:rss:ui`，由 `-PrssFeatureEnabled=true` 单选启用。
- API 公开 RSS 源摘要、分组、打开目标及置顶/停用/删除命令，不泄漏 Room 实体。
- 单 URL、起始 HTML 与 JS URL 的兼容语义集中在 `ResolveRssOpenTargetUseCase`；适配器只映射 API。

## UDF、Effect 与兼容

- RSS 首页、搜索/分组、收藏/规则/管理入口由新 feature UI 持有。
- 文章列表、阅读与收藏详情继续复用根导航 route；外部 URL 只在 app 边界启动 Intent。
- 所有写命令等待既有 Room Flow 回流，且重复操作被串行化；WebView 安全模型未改变。

## 删除条件

RSS DAO/解析接缝下沉并迁移文章、收藏与阅读 route 后删除 `LegacyRssAdapter`、旧首页和灰度开关；发布前签收 JS 单 URL、外链、返回栈与 TalkBack。

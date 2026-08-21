# Catalog Compose 迁移卡（Phase 3.2）

> 状态：实现与范围内自动验证完成；灰度入口默认关闭；设备人工矩阵待发布前执行

## 边界与入口

- 旧入口：主页“发现”分页的 `ExploreRouteScreen`。
- 新入口：`:feature:catalog:ui`，由 `-PcatalogFeatureEnabled=true` 单选启用。
- `:feature:catalog:api` 公开书源摘要、分组、查询状态以及置顶/删除命令；不公开 `BookSourcePart`、DAO 或规则对象。
- `LegacyCatalogAdapter` 从 `ExploreRepository` 的 Flow 回流列表，写命令继续调用既有 Repository；规则执行器未改写。

## UDF、Effect 与兼容

- Screen 提供搜索、分组、发现、限定书源搜索、登录、编辑、导入与管理入口。
- `CatalogRouteScreen` 是 effect 唯一收集者；书籍详情、发现结果与导入子流程仍由 app 的兼容 route 承载。
- 删除和置顶等待 Room SSOT 刷新；重复命令由 ViewModel 串行化。

## 删除条件

书源持久化与规则所需接缝下沉后建立不依赖 app 的 impl；随后迁移发现结果/书籍详情/导入子 route，删除适配器、旧发现首页和灰度开关。

# AI Compose 迁移卡（Phase 3.5）

> 状态：实现与范围内自动验证完成；灰度入口默认关闭；设备人工矩阵待发布前执行

## 边界与入口

- 旧入口：`MainRouteSettingsAi → AiConfigRouteScreen`。
- 新入口：`:feature:ai:ui`，由 `-PaiFeatureEnabled=true` 单选启用。
- API 公开供应商/模型/预设摘要与默认模型命令，不泄漏 Room profile、API key 或协议实现。
- `LegacyAiAdapter` 组合现有 AiProfileGateway 三条 SSOT Flow；默认模型写入仍由该 Gateway 唯一拥有。

## UDF、Effect 与兼容

- 新 UI 覆盖配置总览、默认模型选择以及会话、摘要、提示词和供应商/模型编辑入口。
- Route 是 effect 唯一收集者；会话与生成结果继续由现有 AI route 承载，供应商协议和模型调用未改变。
- 默认模型不做乐观写入，等待 profile/preset Flow 回流；重复点击串行化。

## 删除条件

AI 持久化和生成协议接缝下沉后迁移会话/结果与编辑子 route，删除 `LegacyAiAdapter`、旧配置首页和灰度开关；发布前验证流式生成、取消、工具确认、错误恢复与密钥不泄漏。

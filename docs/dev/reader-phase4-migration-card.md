# Reader Compose 决策门迁移卡（Phase 4）

> 状态：工程实现与范围内自动验证完成；Compose 正文入口默认关闭；发布/默认切换仍需设备矩阵签收
> 日期：2026-08-21

## 决策结论

Phase 4 的结论是“允许继续做受控 parity 实验，但不把 Compose 正文设为默认”。本阶段没有恢复
2026-07-25 删除的 Track C 实现，也没有复用其 overlay、render cache 或可变 `TextPage` 投影。
新的最小闭环以当前 `ReaderSession`、`ReaderViewport` 和 `ChapterProvider` 为 SSOT；设备 parity、
内存、帧率与无障碍门禁全部签收前，默认仍是成熟的 `ReadView`。

## 交付边界

- `:feature:reader:api` 公开只读 `ReaderSnapshot`、`ReaderProgress`、加载/错误/恢复语义和阅读命令；
  不公开 `ReadBook`、`Book`、`TextChapter`、`TextPage`、DAO、Context 或 View。
- `LegacyReaderAdapter` 是唯一模块安全兼容桥。它只投影现有 SSOT，不维护第二份可变会话，也不
  改 Room schema、备份格式、WebDAV 进度格式或服务协议。
- `:feature:reader:ui` 采用 `Contract → ViewModel → Route → Screen`。ViewModel 串行化命令，
  Screen 只接收 state/intent；首帧、viewport 和菜单属于 host 回调。
- 新 renderer 只覆盖普通文字页、上一页/下一页、跨章、loading/error/retry 和菜单点击区域；
  不包含翻页动画、选区、图片/双页、滚动模式、自动翻页或样式 parity。
- 编译期开关为 `-PreaderFeatureEnabled=true`，默认 `false`。关闭时只创建 `ReadBookViewLayer`；
  开启时只创建 `ReaderRouteScreen`，不会把两套正文同时放入 View 树。

## Component Plan 与例外

| 可见元素 | 组件决策 | 证据 |
|---|---|---|
| loading / empty / error / retry | 复用 `core:designsystem` 的 `AppFeedback` | 已覆盖反馈状态与操作按钮 |
| 背景、文字、间距、排版层级 | 复用 `LegadoTheme` token | 与 app shell 的 `ReadBookColorTheme` 同源 |
| 正文页和左右/中央点击区 | 最小自定义 `ReaderPage` | Design System 与 Material 3 均无分页阅读画布；提供页级 contentDescription、菜单 click 与前后页 custom action |
| Snackbar | Material 3 `SnackbarHost` | 标准一次性失败反馈 |

自定义正文画布不重新实现 Button、Card、ListItem、Dialog、Sheet 或导航控件。它没有硬编码颜色、
字号或形状；复杂选择、手势和动画明确不进入本阶段。

## 单一所有权与回滚

```text
feature:reader:ui
        ↓ ReaderSessionGateway
LegacyReaderAdapter（:app 临时桥）
        ↓ immutable projection / controlled command
ReadBook + ReaderSession（现有 SSOT）
        ↓
Room / ChapterProvider / WebDAV（格式不变）
```

`ReaderPhase4BoundaryTest` 固化以下约束：API/UI 不泄漏遗留运行时；主源码只有一个
`ReaderSessionGateway` 实现；两种 renderer 由互斥分支创建。关闭构建开关即可回滚，数据不需要
迁移。若 Compose 路径失败，不保留运行时自动切换或 overlay，避免同一会话双绘/双写。

## 可执行 parity 与性能门禁

自动门禁：

```bash
./gradlew \
  :feature:reader:api:testDebugUnitTest \
  :feature:reader:ui:testDebugUnitTest \
  :feature:reader:ui:lintDebug \
  verifyConfigArchitecture \
  --no-daemon

./gradlew :app:compileAppDebugKotlin \
  -PreaderFeatureEnabled=true \
  --no-daemon
```

真机采集不再调用已删除的实验室运行时开关。先安装默认构建并运行 legacy，记录输出目录；再安装
Compose 构建并用 `--compare-to` 对比：

```bash
python3 tools/capture_reader_phase4_baseline.py \
  --renderer legacy --crop '<正文左>,<正文上>,<正文右>,<正文下>'

python3 tools/capture_reader_phase4_baseline.py \
  --renderer compose \
  --compare-to build/reader-phase4-baseline/<legacy-run> \
  --crop '<正文左>,<正文上>,<正文右>,<正文下>'
```

脚本对每个 renderer 执行前进/返回压力往返，要求前进画面差异大于 1%、回到起点差异不超过
1%，并保存首个非空正文帧、`gfxinfo framestats`、截图和跨 renderer 差异。设备签收至少覆盖：

1. 本地 TXT 与网络纯文字；普通页、章节边界、字体/字号变化、加载失败重试。
2. 手机竖屏、横屏/分屏或平板、大字体、TalkBack、进程恢复。
3. Compose 单栈相对 legacy：首帧 P90 不慢超过 100ms；P95 不超过 32ms；jank 不高超过
   3 个百分点；连续 20 页往返后 PSS 增量不高超过 15%，无持续增长。
4. TalkBack 可读出书名/章节/页码，可执行上一页、下一页和菜单动作；硬件翻页键在无 `ReadView`
   时仍走 UDF 命令。

任何一项未通过，Compose 正文保持非默认并记录证据；不得用提高阈值、保留 overlay 或吞掉失败
来“完成”门禁。

## 删除与后续条件

本阶段不删除 `ReadView`。只有设备门禁在一个稳定版本周期内通过，且图片、双页、滚动、选区、
朗读联动、配置更新与恢复均有 parity 后，才单独评审是否继续扩展 Compose renderer。若继续，
每项能力必须沿 `ReaderSessionGateway` 和不可变快照扩展；不得恢复旧 Track C 文件或直接让
feature UI 读取 `ReadBook`。若不继续，删除 `feature:reader:ui`、兼容桥和构建开关即可，
`reader:api` 可保留为其他 feature 的阅读进度所有权边界。

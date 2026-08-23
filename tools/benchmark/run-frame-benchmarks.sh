#!/usr/bin/env bash
# 跑 FrameTimingBenchmarks，一条命令搞定构建、安装、看门狗和结果输出。
#
#   tools/benchmark/run-frame-benchmarks.sh
#   tools/benchmark/run-frame-benchmarks.sh bookshelfScroll     # 只跑一个
#
# 要点：
#   * 被测应用必须带 -PbenchmarkFixtures=true 构建，否则书架是空的，
#     滚动场景量不到东西（CompilationMode 每轮都会清掉应用数据，
#     只有应用自己在启动时补夹具才活得下来，见 help/BenchmarkFixtures.kt）。
#   * 两个 APK 都用 -g 安装，省掉运行时权限弹窗。
#   * 全程挂着 cta-watchdog.sh 点掉 OEM 首启权限页。
#   * 不用 gradle 的 connectedAndroidTest 来触发，改为直接 am instrument：
#     connectedAndroidTest 跑完会把被测应用卸掉，调试期间很碍事。
set -euo pipefail

cd "$(dirname "$0")/../.."

TEST_CLASS="io.legado.baselineprofile.FrameTimingBenchmarks"
[ $# -gt 0 ] && TEST_CLASS="$TEST_CLASS#$1"

APP_APK_DIR="app/build/outputs/apk/app/benchmarkRelease"
TEST_APK_DIR="baselineProfile/build/outputs/apk/app/benchmarkRelease"

echo "==> 构建被测应用（带书架夹具）"
./gradlew :app:assembleAppBenchmarkRelease -PbenchmarkFixtures=true --console=plain -q

echo "==> 构建 benchmark 测试包"
./gradlew :baselineprofile:assembleAppBenchmarkRelease --console=plain -q

APP_APK=$(ls -t "$APP_APK_DIR"/*arm64*.apk 2>/dev/null | head -1)
TEST_APK=$(ls -t "$TEST_APK_DIR"/*.apk 2>/dev/null | head -1)
[ -n "$APP_APK" ] || { echo "找不到被测应用 APK" >&2; exit 1; }
[ -n "$TEST_APK" ] || { echo "找不到测试 APK" >&2; exit 1; }

echo "==> 安装（-g 授予运行时权限）"
adb install -r -g "$APP_APK"  | tail -1
adb install -r -g "$TEST_APK" | tail -1

APP_ID=$(adb shell pm list packages 2>/dev/null | sed 's/package://' | tr -d '\r' \
    | grep -E '\.benchmark$' | head -1)
[ -n "$APP_ID" ] || { echo "找不到已安装的 benchmark 包" >&2; exit 1; }
echo "    被测包: $APP_ID"

echo "==> 启动 CTA 看门狗"
"$(dirname "$0")/cta-watchdog.sh" &
WATCHDOG=$!
trap 'kill "$WATCHDOG" 2>/dev/null || true' EXIT

echo "==> 跑 $TEST_CLASS"
adb shell am force-stop io.legado.baselineprofile || true
adb shell am instrument -w -r \
    -e class "$TEST_CLASS" \
    -e targetAppId "$APP_ID" \
    io.legado.baselineprofile/androidx.test.runner.AndroidJUnitRunner \
    | tee /tmp/legado-frame-benchmarks.txt \
    | grep -E '^(FrameTimingBenchmarks_|  frame|Tests run|OK |FAILURES)' || true

echo
echo "==> 完整输出: /tmp/legado-frame-benchmarks.txt"

#!/usr/bin/env bash
# 在跑 macrobenchmark 期间，自动点掉 OEM 的首启权限确认页。
#
# 背景：androidx.benchmark 的 CompilationMode 每轮测量前会卸载重装被测应用，
# 部分 OEM（已知索尼 Xperia 的 com.sonymobile.cta，即 /system/app/Access-Management）
# 会在新装应用首次启动时弹出一个盖在应用之上的权限确认页。它挡住整个应用界面，
# UiAutomator 的 By.res 找不到任何应用内节点，测量直接失败或得到空结果。
#
# 已验证无效的规避手段，不用再试：
#   pm install -g                      授予全部运行时权限 —— 照弹
#   pm install -i com.android.vending  伪装成应用市场安装 —— 照弹
# 它只认「这个包是新装的、第一次启动」。
#
# 实现上有一条硬约束：**不能用 uiautomator dump 来定位按钮**。
# macrobenchmark 自己独占 UiAutomation，并发调用会把它打成
# "UiAutomation not connected!" / DeadObjectException，整个跑测卡死。
# 所以检测只用 dumpsys window（纯只读），点击用按屏幕尺寸算出的比例坐标。
#
# 用法：
#   tools/benchmark/cta-watchdog.sh &        # 后台盯着
#   WATCHDOG=$!
#   ...跑 benchmark...
#   kill $WATCHDOG
set -uo pipefail

# 需要点掉的确认页所属包名，按需增补。
CTA_PACKAGES="${CTA_PACKAGES:-com.sonymobile.cta}"
POLL_INTERVAL="${POLL_INTERVAL:-1}"

adb_shell() { adb ${ANDROID_SERIAL:+-s "$ANDROID_SERIAL"} shell "$@"; }

read -r SCREEN_W SCREEN_H < <(
    adb_shell wm size 2>/dev/null |
        sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1
)
if [ -z "${SCREEN_W:-}" ] || [ -z "${SCREEN_H:-}" ]; then
    echo "cta-watchdog: 读不到屏幕尺寸，退出" >&2
    exit 1
fi

# 「确定」按钮固定在右下角。比例取自 1096x2560 上实测的 (1003, 2436)。
TAP_X=$(( SCREEN_W * 915 / 1000 ))
TAP_Y=$(( SCREEN_H * 951 / 1000 ))

echo "cta-watchdog: 屏幕 ${SCREEN_W}x${SCREEN_H}，点击位置 (${TAP_X}, ${TAP_Y})，监视 [${CTA_PACKAGES}]"

dismissed=0
while true; do
    focus=$(adb_shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus || true)
    for pkg in $CTA_PACKAGES; do
        case "$focus" in
            *"$pkg"*)
                dismissed=$((dismissed + 1))
                echo "cta-watchdog: 检测到 $pkg，点掉（第 ${dismissed} 次）"
                adb_shell input tap "$TAP_X" "$TAP_Y" >/dev/null 2>&1
                sleep 2
                ;;
        esac
    done
    sleep "$POLL_INTERVAL"
done

#!/usr/bin/env python3
"""Capture Phase 4 single-renderer reader parity, navigation and frame evidence.

Install one APK first, then run this script with the renderer used to build it. Run once for
the default legacy build and once for `-PreaderFeatureEnabled=true`; pass the legacy output to
the Compose run with `--compare-to` to record static screenshot differences.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

sys.dont_write_bytecode = True

from capture_reader_c3_baseline import (  # noqa: E402
    Adb,
    BaselineError,
    MAIN_ACTIVITY_CLASS,
    PACKAGE_DEFAULT,
    READ_ROUTE,
    capture,
    compare_pngs,
    connected_serial,
    parse_crop,
    parse_framestats,
    require_unlocked,
    screen_size,
)

FIRST_FRAME_EXTRA = "readerFirstFrameStartedAtNanos"


def elapsed_realtime_nanos(adb: Adb) -> int:
    seconds = float(adb.run("shell", "cat", "/proc/uptime").split()[0])
    return int(seconds * 1_000_000_000)


def open_reader(adb: Adb, package: str, book_url: str | None, started_at: int) -> None:
    command = [
        "shell", "am", "start", "-S", "-W", "-n",
        f"{package}/{MAIN_ACTIVITY_CLASS}",
        "--es", "startRoute", READ_ROUTE,
        "--el", FIRST_FRAME_EXTRA, str(started_at),
    ]
    if book_url:
        command.extend(["--es", "bookUrl", book_url])
    adb.run(*command)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--renderer", required=True, choices=("legacy", "compose"))
    parser.add_argument("--book-url")
    parser.add_argument("--serial")
    parser.add_argument("--package", default=PACKAGE_DEFAULT)
    parser.add_argument("--turns", type=int, default=10)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--tap-delay", type=float, default=0.18)
    parser.add_argument("--settle-seconds", type=float, default=1.5)
    parser.add_argument("--crop", type=parse_crop)
    parser.add_argument("--compare-to", type=Path, help="另一 renderer 的证据目录")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    if args.turns < 1 or args.rounds < 1:
        parser.error("--turns 与 --rounds 必须大于 0")

    serial = connected_serial(args.serial)
    adb = Adb(serial)
    require_unlocked(adb)
    output = args.out or Path("build/reader-phase4-baseline") / (
        datetime.now().strftime("%Y%m%d-%H%M%S") + f"-{args.renderer}"
    )
    output.mkdir(parents=True, exist_ok=False)

    adb.run("logcat", "-c")
    started_at = elapsed_realtime_nanos(adb)
    open_reader(adb, args.package, args.book_url, started_at)
    time.sleep(args.settle_seconds)
    size = screen_size(adb)
    plain = output / "plain.png"
    forward = output / "forward.png"
    returned = output / "returned.png"
    capture(adb, plain)

    adb.run("shell", "dumpsys", "gfxinfo", args.package, "reset")
    next_xy = (str(int(size[0] * 0.84)), str(int(size[1] * 0.50)))
    previous_xy = (str(int(size[0] * 0.16)), str(int(size[1] * 0.50)))
    forward_difference = None
    for round_index in range(args.rounds):
        for _ in range(args.turns):
            adb.run("shell", "input", "tap", *next_xy)
            time.sleep(args.tap_delay)
        if round_index == 0:
            time.sleep(args.settle_seconds)
            capture(adb, forward)
            forward_difference = compare_pngs(plain, forward, args.crop)
            if forward_difference["changedPixelPercent"] < 1.0:
                raise BaselineError("前进后正文画面未发生有效变化")
        for _ in range(args.turns):
            adb.run("shell", "input", "tap", *previous_xy)
            time.sleep(args.tap_delay)

    time.sleep(args.settle_seconds)
    capture(adb, returned)
    return_difference = compare_pngs(plain, returned, args.crop)
    if return_difference["changedPixelPercent"] > 1.0:
        raise BaselineError(
            f"往返后未回到起点：差异像素 {return_difference['changedPixelPercent']:.2f}%"
        )

    frame_path = output / "framestats.txt"
    frame_path.write_text(
        adb.run("shell", "dumpsys", "gfxinfo", args.package, "framestats"),
        encoding="utf-8",
    )
    logs = adb.run("logcat", "-d", "-s", "ReaderFirstFrame:I", "*:S")
    marker = re.search(r"renderer=(\w+)\s+durationMs=([\d.]+)", logs)
    if marker is None or marker.group(1) != args.renderer:
        raise BaselineError(
            f"首帧标记与 --renderer 不符；期望 {args.renderer}，日志为：{logs.strip()}"
        )

    result: dict[str, object] = {
        "serial": serial,
        "package": args.package,
        "renderer": args.renderer,
        "bookUrl": args.book_url,
        "screen": list(size),
        "turns": args.turns,
        "rounds": args.rounds,
        "crop": args.crop,
        "plainVsForward": forward_difference,
        "plainVsReturned": return_difference,
        "frameStats": parse_framestats(frame_path),
        "firstFrameMs": float(marker.group(2)),
    }
    if args.compare_to:
        result["otherRendererPlainDifference"] = compare_pngs(
            args.compare_to / "plain.png", plain, args.crop
        )
        result["otherRendererReturnedDifference"] = compare_pngs(
            args.compare_to / "returned.png", returned, args.crop
        )
    (output / "result.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps({"output": str(output), **result}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (BaselineError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
        print(f"Phase 4 阅读器基线采集失败：{error}", file=sys.stderr)
        raise SystemExit(2)
